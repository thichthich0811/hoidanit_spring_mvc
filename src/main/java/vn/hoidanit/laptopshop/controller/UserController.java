package vn.hoidanit.laptopshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

// @RestController
// public class UserController {
//     @GetMapping("/")
//     public String index() {
//         return "Hello World update!";
//     }

//     @GetMapping("/user")
//     public String userPage() {
//         return "Only user can access this page!";
//     }

//     @GetMapping("/admin")
//     public String adminPage() {
//         return "Only admin can access this page!";
//     }
// }
@RestController
public class UserController {
    @RequestMapping("/")
    public String getHomePage() {
        return "thich.html";
    }

}