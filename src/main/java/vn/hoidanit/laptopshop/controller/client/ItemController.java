package vn.hoidanit.laptopshop.controller.client;

import java.lang.StackWalker.Option;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import vn.hoidanit.laptopshop.domain.Cart;
import vn.hoidanit.laptopshop.domain.CartDetail;
import vn.hoidanit.laptopshop.domain.Product;
import vn.hoidanit.laptopshop.domain.Product_;
import vn.hoidanit.laptopshop.domain.User;
import vn.hoidanit.laptopshop.domain.dto.ProductCriteriaDTO;
import vn.hoidanit.laptopshop.service.ProductService;
import vn.hoidanit.laptopshop.service.UserService;

@Controller
public class ItemController {
    private final ProductService productService;
    // private final UserService userService;

    public ItemController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/product/{id}")
    public String getProductPage(Model Model, @PathVariable Long id) {
        Product product = productService.getProductById(id).get();
        Model.addAttribute("product", product);
        return "client/product/detail";
    }

    @PostMapping("/add-product-to-cart/{id}")
    public String addProductToCart(@PathVariable Long id, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String email = (String) session.getAttribute("email");
        long productId = id;
        this.productService.handleAddProductToCart(email, id, session, (long) 1);

        return "redirect:/";
    }

    @GetMapping("/cart")
    public String getCartPage(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Long id = (Long) session.getAttribute("id");
        // Optional<User> userOptional = this.userService.getById(id);
        // User user = userOptional.get();
        User user = new User();
        user.setId(id);
        Cart cart = this.productService.fetchByUser(user);
        List<CartDetail> cartDetail = cart == null ? new ArrayList<CartDetail>() : cart.getCartDetails();
        double totalPrice = 0;
        for (CartDetail cd : cartDetail) {
            totalPrice += cd.getPrice() * cd.getQuantity();
        }
        model.addAttribute("cartDetails", cartDetail);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("cart", cart);
        return "client/cart/show";
    }

    @PostMapping("/delete-cart-product/{id}")
    public String deleteCartProduct(@PathVariable Long id, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        long cartDetailId = id;
        this.productService.handleRemoveCartDetail(id, session);
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String getCheckOutPage(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Long id = (Long) session.getAttribute("id");
        User user = new User();
        user.setId(id);
        Cart cart = this.productService.fetchByUser(user);
        List<CartDetail> cartDetails = cart == null ? new ArrayList<CartDetail>() : cart.getCartDetails();
        double totalPrice = 0;
        for (CartDetail c : cartDetails) {
            totalPrice += c.getPrice() * c.getQuantity();
        }
        model.addAttribute("cartDetails", cartDetails);
        model.addAttribute("totalPrice", totalPrice);
        return "client/cart/checkout";
    }

    @PostMapping("/confirm-checkout")
    public String getCheckOutPage(@ModelAttribute("cart") Cart cart) {
        List<CartDetail> cartDetails = cart == null ? new ArrayList<CartDetail>() : cart.getCartDetails();
        this.productService.handleUpdateCartBeforeCheckout(cartDetails);
        return "redirect:/checkout";
    }

    @PostMapping("/place-order")
    public String handlePlaceOrder(
            HttpServletRequest request,
            @RequestParam("receiverName") String receiverName,
            @RequestParam("receiverAddress") String receiverAddress,
            @RequestParam("receiverPhone") String receiverPhone) {
        HttpSession session = request.getSession(false);
        User currentUser = new User();
        currentUser.setId((Long) session.getAttribute("id"));
        this.productService.handlePlaceOrder(currentUser, session, receiverName, receiverAddress, receiverPhone);

        return "redirect:/thanks";
    }

    @GetMapping("/thanks")
    public String getThanksPage() {
        return "client/cart/thanks";
    }

    @PostMapping("/add-product-from-view-detail")
    public String handleAddProductFromViewDetail(
            @RequestParam("id") long id,
            @RequestParam("quantity") long quantity,
            HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        String email = (String) session.getAttribute("email");
        this.productService.handleAddProductToCart(email, id, session, quantity);
        return "redirect:/product/" + id;
    }

    @GetMapping("/products")
    public String getProductPage(Model model, ProductCriteriaDTO productCriteriaDTO,
            HttpServletRequest httpServletRequest) {
        int page = 1;
        try {
            if (productCriteriaDTO.getPage().isPresent()) {
                // convert from String to int
                page = Integer.parseInt(productCriteriaDTO.getPage().get());
            } else {
                // page = 1
            }
        } catch (Exception e) {
            // page = 1
            // TODO: handle exception
        }
        Pageable pageable = PageRequest.of(page - 1, 3);
        if (productCriteriaDTO.getSort() != null && productCriteriaDTO.getSort().isPresent()) {
            String sort = productCriteriaDTO.getSort().get();
            if (sort.equals("gia-tang-dan")) {
                pageable = PageRequest.of(page - 1, 3, Sort.by(Product_.PRICE).ascending());
            } else if (sort.equals("gia-giam-dan")) {
                pageable = PageRequest.of(page - 1, 3, Sort.by(Product_.PRICE).descending());
            }
        }

        Page<Product> prs = this.productService.getAllProductsWithSpec(pageable, productCriteriaDTO);

        // case 1: Lấy ra tất cả sản phẩm có giá cả tối thiểu
        // double min = minPriceOptional.isPresent() ?
        // Double.parseDouble(minPriceOptional.get()) : 0;
        // Page<Product> prs = this.productService.getAllProductsWithSpec(pageable,
        // min);

        // case 2: Lấy ra tất cả sản phẩm có giá cả tối đa
        // double max = maxPriceOptional.isPresent() ?
        // Double.parseDouble(maxPriceOptional.get()) : 0;
        // Page<Product> prs = this.productService.getAllProductsWithSpec(pageable,
        // max);

        // case 3: Lấy ra tất cả sản phẩm có hãng sản xuất = APPLE
        // String factory = factoryOptional.isPresent() ? factoryOptional.get() : "";
        // Page<Product> prs = this.productService.getAllProductsWithSpec(pageable,
        // factory);

        // case 4: Lấy ra tất cả sản phẩm có hãng sản xuất = APPLE hoặc DELL . Truyền
        // nhiều điều kiện,
        // ngăn cách các giá trị bởi dấu phẩy (điều kiện IN)
        // List<String> factory = Arrays.asList(factoryOptional.get().split(","));
        // Page<Product> prs = this.productService.getAllProductsWithSpec(pageable,
        // factory);

        // case 5: Lấy ra tất cả sản phẩm theo range (khoảng giá). 10 triệu <= price <=
        // 15 triệu
        // String price = priceOptional.isPresent() ? priceOptional.get() : "";
        // Page<Product> prs = this.productService.getAllProductsWithSpec(pageable,
        // price);

        // case 6
        // List<String> price = Arrays.asList(priceOptional.get().split(","));
        // Page<Product> prs = this.productService.fetchProductsWithSpec(pageable,
        // price);

        List<Product> products = prs.getContent().size() > 0 ? prs.getContent() : new ArrayList<Product>();

        String qs = httpServletRequest.getQueryString();
        if (qs != null && !qs.isBlank()) {
            // remove page
            qs = qs.replace("page=" + page, "");
        }
        model.addAttribute("products", products);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", prs.getTotalPages());
        model.addAttribute("queryString", qs);
        return "client/product/show";
    }
}
