package vn.fs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.fs.entities.Category;
import vn.fs.repository.CategoryRepository;

import java.util.List;

@Controller
public class CommomController {

    @Autowired
    CategoryRepository categoryRepository;

    @ModelAttribute("categoryList")
    public List<Category> showCategory(Model model) {
        List<Category> categoryList = categoryRepository.findAll();
        model.addAttribute("categoryList", categoryList);
        return categoryList;
    }
}
