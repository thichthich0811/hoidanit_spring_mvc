package vn.hoidanit.laptopshop.controller.admin;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.ServletContext;
import jakarta.validation.Valid;
import vn.hoidanit.laptopshop.domain.Product;
import vn.hoidanit.laptopshop.domain.User;
import vn.hoidanit.laptopshop.repository.UserRepository;
import vn.hoidanit.laptopshop.service.UploadService;
import vn.hoidanit.laptopshop.service.UserService;

@Controller
public class UserController {
    private final UserService userService;
    private final UploadService uploadService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, UploadService uploadService,
            PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.uploadService = uploadService;
        this.passwordEncoder = passwordEncoder;
    }

    // @RequestMapping("/")
    // public String getHomePage(Model model) {
    // List<User> arrUsers =
    // this.userService.getAllUserByEmail("thichn12@gmail.com");
    // // System.out.println(arrUsers);
    // model.addAttribute("s1", "jsp");
    // model.addAttribute("s2", "java spring");
    // return "hello";
    // }

    @RequestMapping("/admin/user")
    public String getUserPage(Model model) {
        List<User> users = userService.getAllUser();
        model.addAttribute("users1", users);
        return "admin/user/show";
    }

    @RequestMapping("/admin/user/{id}")
    public String getUserDetailPage(@PathVariable Long id, Model model) {
        User user = userService.getById(id).get();
        model.addAttribute("user", user);
        return "admin/user/detail";
    }

    @GetMapping("/admin/user/create")
    public String getCreateUserPage(Model model) {
        model.addAttribute("newUser", new User());
        return "admin/user/create";
    }

    @PostMapping(value = "/admin/user/create")
    public String postCrateUserPage(Model model,
            @ModelAttribute("newUser") @Valid User u1,
            BindingResult newUserbindingResult,
            @RequestParam("hoidanitFile") MultipartFile file) {

        // validate
        // List<FieldError> errors = newUserbindingResult.getFieldErrors();
        // for (FieldError error : errors) {
        // System.out.println(error.getField() + " - " + error.getDefaultMessage());
        // }
        if (newUserbindingResult.hasErrors()) {
            return "admin/user/create";
        }
        String avatar = this.uploadService.handleSaveUploadFile(file, "avatar");
        String hashPassword = this.passwordEncoder.encode(u1.getPassword());
        u1.setAvatar(avatar);
        u1.setPassword(hashPassword);
        u1.setRole(userService.getRoleByName(u1.getRole().getName()));
        this.userService.handleSaveUser(u1);
        return "redirect:/admin/user";
    }

    @GetMapping("/admin/user/update/{id}")
    public String getUpdateUserPage(Model model, @PathVariable Long id) {
        Optional<User> currentUser = this.userService.getById(id);
        model.addAttribute("newUser", currentUser.get());
        return "admin/user/update";
    }

    @PostMapping("/admin/user/update")
    public String postUpdateUserPage(@ModelAttribute("newUser") @Valid User ur,
            BindingResult newUserBindingResult,
            @RequestParam("hoidanitFile") MultipartFile file) {

        // validate
        if (newUserBindingResult.hasErrors()) {
            return "admin/user/update";
        }

        User currentUser = this.userService.getById(ur.getId()).get();
        if (currentUser != null) {
            // update new image
            if (!file.isEmpty()) {
                String img = this.uploadService.handleSaveUploadFile(file, "avatar");
                currentUser.setAvatar(img);
            }
            currentUser.setAddress(ur.getAddress());
            currentUser.setEmail(ur.getEmail());
            currentUser.setFullName(ur.getFullName());
            currentUser.setPhone(ur.getPhone());
            // currentUser.setPassword(this.passwordEncoder.encode(ur.getPassword()));
            currentUser.setRole(userService.getRoleByName(ur.getRole().getName()));
            this.userService.handleSaveUser(currentUser);
        }

        return "redirect:/admin/user";
    }

    @GetMapping("/admin/user/delete/{id}")
    public String getDeleteUserPage(@PathVariable Long id, Model model) {
        model.addAttribute("id", id);
        model.addAttribute("newUser", new User());
        return "admin/user/delete";
    }

    @PostMapping("/admin/user/delete")
    public String postDeleteUserPage(@ModelAttribute("newUser") User u1, Model model) {
        userService.deleteUserById(u1.getId());
        return "redirect:/admin/user";
    }

}