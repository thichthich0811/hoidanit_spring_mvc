package vn.hoidanit.laptopshop.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import vn.hoidanit.laptopshop.service.UserService;

@Controller
public class DashboardController {
    private final UserService userService;

    public DashboardController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/admin")
    public String getDashBoard(Model model) {
        model.addAttribute("countUsers", this.userService.getCountUser());
        model.addAttribute("countOrders", this.userService.getCountOrder());
        model.addAttribute("countProducts", this.userService.getCountProduct());
        return "admin/dashboard/show";
    }

}
