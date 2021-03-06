package com.akiponkuma.blog.presentation.controller;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.akiponkuma.blog.domain.model.entity.Post;
import com.akiponkuma.blog.infrastructure.dao.PostDao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Controller
@RequestMapping("/post")
public class PostController {
	@Autowired
	private PostDao postDao;
	
	@Autowired
	private ConnectionRepository connectionRepository;
	
	@RequestMapping(value="/write", method=RequestMethod.GET)
	public String form(Post post){
		post.setRegDate(LocalDateTime.now());
		return "post/form";
	}
	
	@RequestMapping(value="/write", method=RequestMethod.POST)
	public String write(@Valid Post post, BindingResult bindingResult){
		User user = getConnect();		
		if(bindingResult.hasErrors()){
			return "post/form";
		}		
		post.setRegDate(LocalDateTime.now());
		post.setUserId(user.getProviderUserId());
		post.setName(user.getDisplayName());
		return "redirect:/post/" + postDao.save(post).getId();
	}
	

	@RequestMapping("/list")
	public String list(Model model, @PageableDefault(sort={"id"}, direction=Direction.DESC, size=2) Pageable pageable){
		Page<Post> postPage = postDao.findAll(pageable);
		User user = getConnect();
		model.addAttribute("user", user);
		model.addAttribute("postPage", postPage);
		return "post/list";
	}
	
	@RequestMapping("/{id}")
	public String view(Model model, @PathVariable int id){
		Post post = postDao.findOne(id);
		User user = getConnect();
		model.addAttribute("user", user);
		model.addAttribute("post", post);
		return "post/post";
	}
	
	@RequestMapping("/{id}/delete")
	public String delete(@PathVariable int id){
		User user = getConnect();
		Post post = postDao.findOne(id);
		if(post.getUserId().equals(user.getProviderUserId())){
			postDao.delete(id);			
		}		
		return "redirect:/post/list";
	}
	
	@RequestMapping(value="/{id}/edit", method=RequestMethod.GET)
	public String editor(Model model, @PathVariable int id){
		Post post = postDao.findOne(id);
		model.addAttribute("post", post);
		return "post/form";
	}
	
	@RequestMapping(value="/{id}/edit", method=RequestMethod.POST)
	public String edit(@Valid Post post, BindingResult bindingResult){
		User user = getConnect();
		if(bindingResult.hasErrors()){
			return "post/form";		
		}
		
		Post oldPost = postDao.findOne(post.getId());
		if(oldPost.getUserId().equals(user.getProviderUserId())){
			oldPost.setTitle(post.getTitle());
			oldPost.setSubtitle(post.getSubtitle());
			oldPost.setContent(post.getContent());
			return "redirect:/post/" + postDao.save(oldPost).getId();
		}
		return "post/form";
	}
	
	private User getConnect() {
		Connection<Facebook> connection = connectionRepository.findPrimaryConnection(Facebook.class);
		if(connection == null){
			return null;			
		}
		ConnectionData data = connection.createData();
		return new User(data.getProviderUserId(), data.getDisplayName());
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class User {
		String providerUserId;
		String displayName;
	}
}