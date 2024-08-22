package vn.hoidanit.laptopshop.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;
import vn.hoidanit.laptopshop.domain.Cart;
import vn.hoidanit.laptopshop.domain.CartDetail;
import vn.hoidanit.laptopshop.domain.Order;
import vn.hoidanit.laptopshop.domain.OrderDetail;
import vn.hoidanit.laptopshop.domain.Product;
import vn.hoidanit.laptopshop.domain.Product_;
import vn.hoidanit.laptopshop.domain.User;
import vn.hoidanit.laptopshop.domain.dto.ProductCriteriaDTO;
import vn.hoidanit.laptopshop.repository.CartDetailRepository;
import vn.hoidanit.laptopshop.repository.CartRepository;
import vn.hoidanit.laptopshop.repository.OrderDetailRepository;
import vn.hoidanit.laptopshop.repository.OrderRepository;
import vn.hoidanit.laptopshop.repository.ProductRepository;
import vn.hoidanit.laptopshop.service.specification.ProductSpecs;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartDetailRepository cartDetailRepository;
    private final UserService userService;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;

    public ProductService(ProductRepository productRepository,
            CartRepository cartRepository,
            CartDetailRepository cartDetailRepository,
            UserService userService,
            OrderRepository orderRepository,
            OrderDetailRepository orderDetailRepository) {
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.cartDetailRepository = cartDetailRepository;
        this.userService = userService;
        this.orderDetailRepository = orderDetailRepository;
        this.orderRepository = orderRepository;
    }

    public Page<Product> getAllProducts(Pageable page) {
        return productRepository.findAll(page);
    }

    public Page<Product> getAllProductsWithSpec(Pageable page, ProductCriteriaDTO productCriteriaDTO) {
        Specification<Product> combinedSpec = Specification.where(null);
        if (productCriteriaDTO.getFactory() == null
                && productCriteriaDTO.getTarget() == null
                && productCriteriaDTO.getPrice() == null) {
            return productRepository.findAll(page);
        }
        if (productCriteriaDTO.getFactory() != null && productCriteriaDTO.getFactory().isPresent()) {
            Specification<Product> spec = ProductSpecs.matchListFactory(productCriteriaDTO.getFactory().get());
            combinedSpec = combinedSpec.and(spec);
        }
        if (productCriteriaDTO.getTarget() != null && productCriteriaDTO.getTarget().isPresent()) {
            Specification<Product> spec = ProductSpecs.matchListTarget(productCriteriaDTO.getTarget().get());
            combinedSpec = combinedSpec.and(spec);
        }
        if (productCriteriaDTO.getPrice() != null && productCriteriaDTO.getPrice().isPresent()) {
            Specification<Product> spec = this.buildPriceSpectification(productCriteriaDTO.getPrice().get());
            combinedSpec = combinedSpec.and(spec);
        }

        return productRepository.findAll(combinedSpec, page);
    }

    // case 1:
    // public Page<Product> getAllProductsWithSpec(Pageable page, double min) {
    // return productRepository.findAll(ProductSpecs.minPrice(min), page);
    // }

    // case 2:
    // public Page<Product> getAllProductsWithSpec(Pageable page, double max) {
    // return productRepository.findAll(ProductSpecs.maxPrice(max), page);
    // }

    // case 3:
    // public Page<Product> getAllProductsWithSpec(Pageable page, String factory) {
    // return productRepository.findAll(ProductSpecs.matchFactory(factory), page);
    // }

    // case 4:
    // public Page<Product> getAllProductsWithSpec(Pageable page, List<String>
    // factory) {
    // return productRepository.findAll(ProductSpecs.matchListFactory(factory),
    // page);
    // }

    // case 5
    // public Page<Product> getAllProductsWithSpec(Pageable page, String price) {

    // if (price.equals("10-toi-15-trieu")) {
    // return productRepository.findAll(ProductSpecs.priceBetween((double) 10000000,
    // (double) 15000000), page);
    // } else if (price.equals("15-toi-30-trieu")) {
    // return productRepository.findAll(ProductSpecs.priceBetween((double) 15000000,
    // (double) 30000000), page);
    // }
    // return getAllProducts(page);
    // }

    // case 6
    public Specification<Product> buildPriceSpectification(List<String> price) {
        Specification<Product> combinedSpec = Specification.where(null);
        for (String p : price) {
            double min = 0;
            double max = 0;

            // Set the appropriate min and max based on the price range string
            switch (p) {
                case "duoi-10-trieu":
                    max = 10000000;
                    min = 1;
                    break;
                case "10-15-trieu":
                    min = 10000000;
                    max = 15000000;
                    break;
                case "15-20-trieu":
                    min = 15000000;
                    max = 20000000;
                    break;
                case "tren-20-trieu":
                    min = 20000000;
                    max = 300000000;
                    break;
                // Add more cases as needed
            }

            if (min != 0 && max != 0) {
                Specification<Product> rangeSpec = ProductSpecs.matchMultiplePrice(min, max);
                combinedSpec = combinedSpec.or(rangeSpec);
            }
        }
        return combinedSpec;
    }

    public Product handleSaveProduct(Product product) {
        return productRepository.save(product);
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public void deleteProductById(Long id) {
        this.productRepository.deleteById(id);
    }

    public void handleAddProductToCart(String email, Long productId, HttpSession session, Long quantity) {
        User user = this.userService.getUserByEmail(email);
        if (user != null) {
            // check user đã có Cart chưa ? nếu chưa -> tạo mới
            Cart cart = this.cartRepository.findByUser(user);

            if (cart == null) {
                // tạo mới cart
                Cart otherCart = new Cart();
                otherCart.setUser(user);
                otherCart.setSum(0);

                cart = this.cartRepository.save(otherCart);
            }

            // save cart_detail
            // tìm product by id

            Optional<Product> productOptional = this.productRepository.findById(productId);
            if (productOptional.isPresent()) {
                Product realProduct = productOptional.get();

                // check sản phẩm đã từng được thêm vào giỏ hàng trước đây chưa ?
                CartDetail oldDetail = this.cartDetailRepository.findByCartAndProduct(cart, realProduct);
                //
                if (oldDetail == null) {
                    CartDetail cd = new CartDetail();
                    cd.setCart(cart);
                    cd.setProduct(realProduct);
                    cd.setPrice(realProduct.getPrice());
                    cd.setQuantity(quantity);
                    this.cartDetailRepository.save(cd);

                    // update cart (sum);
                    int s = cart.getSum() + 1;
                    cart.setSum(s);
                    this.cartRepository.save(cart);
                    session.setAttribute("sum", s);
                } else {
                    oldDetail.setQuantity(oldDetail.getQuantity() + quantity);
                    this.cartDetailRepository.save(oldDetail);
                }

            }
        }
    }

    public Cart fetchByUser(User user) {
        return this.cartRepository.findByUser(user);
    }

    public void handleRemoveCartDetail(Long id, HttpSession session) {
        // TODO Auto-generated method stub
        Optional<CartDetail> optional = this.cartDetailRepository.findById(id);
        if (optional.isPresent()) {
            CartDetail cartDetail = optional.get();
            Cart cart = cartDetail.getCart();
            this.cartDetailRepository.delete(cartDetail);
            // update cart
            if (cart.getSum() > 1) {
                int s = cart.getSum() - 1;
                cart.setSum(s);
                this.cartRepository.save(cart);
                session.setAttribute("sum", s);
            } else {
                this.cartRepository.delete(cart);
                session.setAttribute("sum", 0);
            }
        }
    }

    public void handleUpdateCartBeforeCheckout(List<CartDetail> cartDetails) {
        // TODO Auto-generated method stub
        for (CartDetail cartDetail : cartDetails) {
            Optional<CartDetail> cdOptional = this.cartDetailRepository.findById(cartDetail.getId());
            if (cdOptional.isPresent()) {
                CartDetail currentCartDetail = cdOptional.get();
                currentCartDetail.setQuantity(cartDetail.getQuantity());
                this.cartDetailRepository.save(currentCartDetail);
            }
        }
    }

    public void handlePlaceOrder(User user, HttpSession session,
            String receiverName, String receiverAddress, String receiverPhone) {

        // step 1:get cart by user
        Cart cart = this.cartRepository.findByUser(user);
        if (cart != null) {
            List<CartDetail> cartDetails = cart.getCartDetails();
            if (cartDetails != null) {
                // create order
                Order order = new Order();
                order.setUser(user);
                order.setReceiverName(receiverName);
                order.setReceiverAddress(receiverAddress);
                order.setReceiverPhone(receiverPhone);
                order.setStatus("PENDING");
                double sum = 0;
                for (CartDetail cd : cartDetails) {
                    sum += cd.getPrice() * cd.getQuantity();
                }
                order.setTotalPrice(sum);
                order = this.orderRepository.save(order);

                // create orderDetail
                for (CartDetail cd : cartDetails) {
                    OrderDetail orderDetail = new OrderDetail();
                    orderDetail.setOrder(order);
                    orderDetail.setProduct(cd.getProduct());
                    orderDetail.setQuantity(cd.getQuantity());
                    orderDetail.setPrice(cd.getPrice());
                    this.orderDetailRepository.save(orderDetail);
                }
                // step 2: delete cart_detail and cart
                for (CartDetail cd : cartDetails) {
                    this.cartDetailRepository.delete(cd);
                }
                this.cartRepository.delete(cart);
                // step 3: update session
                session.setAttribute("sum", 0);
            }
        }

    }

}
