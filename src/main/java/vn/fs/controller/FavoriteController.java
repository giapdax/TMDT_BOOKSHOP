package vn.fs.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import vn.fs.commom.CommomDataService;
import vn.fs.entities.Favorite;
import vn.fs.entities.Product;
import vn.fs.entities.User;
import vn.fs.repository.FavoriteRepository;
import vn.fs.repository.ProductRepository;
import vn.fs.repository.UserRepository;
import vn.fs.util.SessionUtils;

import javax.servlet.http.HttpSession;


@Controller
public class FavoriteController extends CommomController {

	@Autowired
	FavoriteRepository favoriteRepository;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	CommomDataService commomDataService;


	@GetMapping(value = "/favorite")
	public String favorite(Model model, User user) {
		List<Favorite> favorites = favoriteRepository.selectAllSaves(user.getUserId());
		commomDataService.commonData(model, user);
		model.addAttribute("favorites", favorites);
		return "web/favorite";
	}

	@GetMapping("/doFavorite")
	public String doFavorite(User user,
							 @RequestParam("id") Long id,
							 @RequestParam(value = "redirect", defaultValue = "/products") String redirect) {
		Product product = productRepository.findById(id).orElse(null);
		if (product != null && user != null) {
			Favorite favorite = new Favorite();
			favorite.setProduct(product);
			favorite.setUser(user);
			favoriteRepository.save(favorite);
		}
		return "redirect:" + redirect;
	}
	@GetMapping("/doUnFavorite")
	public String doUnFavorite(User user,
							 @RequestParam("id") Long id,
							 @RequestParam(value = "redirect", defaultValue = "/products") String redirect) {
		Favorite favorite = favoriteRepository.selectSaves(id, user.getUserId());
		if (favorite != null) {
			favoriteRepository.delete(favorite);
		}
		return "redirect:" + redirect;
	}
}
