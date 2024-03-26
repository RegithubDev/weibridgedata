package com.resustainability.reirm.RESTful;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.resustainability.reisp.common.DateForUser;
import com.resustainability.reisp.constants.PageConstants;
import com.resustainability.reisp.model.Project;
import com.resustainability.reisp.model.RoleMapping;
import com.resustainability.reisp.model.User;
import com.resustainability.reisp.service.LocationService;
import com.resustainability.reisp.service.ProjectService;
import com.resustainability.reisp.service.RoleMappingService;
import com.resustainability.reisp.service.UserService;


@CrossOrigin(origins="*" ,maxAge = 3600)
@RestController
@RequestMapping("/mobileapp")
public class RESTfullController {
	Logger logger = Logger.getLogger(RESTfullController.class);

	@Autowired
	UserService service;
	
	@Autowired
	UserService service2;
	
	@Autowired
	LocationService service3;
	
	@Autowired
	RoleMappingService service4;
	
	@Autowired
	ProjectService service5;
	
	@Value("${Logout.Message}")
	private String logOutMessage;
	
	@Value("${Login.Form.Invalid}")
	public String invalidUserName;
	
	
	@Value("${common.error.message}")
	public String commonError;
	
	@RequestMapping(value = "/", method = {RequestMethod.POST, RequestMethod.GET})
	public ModelAndView basePage(@ModelAttribute User user, HttpSession session,HttpServletRequest request) {
		ModelAndView model = new ModelAndView(PageConstants.Applogin);
		try {
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return model;
	}
	
	
	@RequestMapping(value = "/app-login", method = {RequestMethod.POST, RequestMethod.GET})
	public ModelAndView login(@ModelAttribute User user, HttpSession session,HttpServletRequest request,RedirectAttributes attributes) {
		ModelAndView model = new ModelAndView(PageConstants.Applogin);
		User userDetails = null;
		try {
			if(!StringUtils.isEmpty(user) && !StringUtils.isEmpty(user.getEmail_id())){
				user.setUser_session_id(user.getUser_session_id());
				userDetails = service.validateUser(user);
				if(!StringUtils.isEmpty(userDetails)) {
					//if((userDetails.getSession_count()) == 0) {
						model.setViewName("redirect:/reone/home");
						User permisions = service.getAllPermissions(userDetails.getBase_role());
						/// USER PERMISISONS
						session.setAttribute("R_ADD", permisions.getP_add());
						session.setAttribute("R_EDIT", permisions.getP_edit());
						session.setAttribute("R_VIEW", permisions.getP_view());
						session.setAttribute("R_APPROVALS", permisions.getP_approvals());
						session.setAttribute("R_REPORTS", permisions.getP_reports());
						session.setAttribute("R_DASHBOARD", permisions.getP_dashboards());
						session.setAttribute("R_AUTO_EMAIL", permisions.getP_auto_email());
						/// USER BASIC SESSION DATA
						session.setAttribute("user", userDetails);
						session.setAttribute("ID", userDetails.getId());
						session.setAttribute("USER_ID", userDetails.getUser_id());
						session.setAttribute("USER_NAME", userDetails.getUser_name());
						session.setAttribute("USER_EMAIL", userDetails.getEmail_id());
						session.setAttribute("BASE_ROLE", userDetails.getBase_role());
						session.setAttribute("USER_IMAGE", user.getProfileImg());
						session.setAttribute("REPORTING_TO", user.getReporting_to());
						session.setAttribute("BASE_SBU", userDetails.getBase_sbu());
						session.setAttribute("BASE_PROJECT", userDetails.getProject_name());
						session.setAttribute("BASE_DEPARTMENT", userDetails.getBase_department());
						session.setAttribute("REWARDS", userDetails.getReward_points());
						session.setAttribute("BASE_PROJECT_CODE", userDetails.getBase_project());
						session.setAttribute("CURRENT_PROJECT", user.getCurrent_project());
						session.setAttribute("SESSION_ID", user.getUser_session_id());
						List<User> menuList = service.getMenuList();
						session.setAttribute("menuList", menuList);
						attributes.addFlashAttribute("welcome", "welcome "+userDetails.getUser_name());
					//}else {
						//session.invalidate();
						//model.addObject("multipleLoginFound","Multiple Login found! You have been Logged out from all Devices");
						//model.setViewName(PageConstants.login); 
					//}
				}else{
					model.addObject("invalidEmail",invalidUserName);
					model.setViewName(PageConstants.newUserLogin);
					List<RoleMapping> projectsList = service4.getProjectsList(null);
					model.addObject("projectsList", projectsList);
					
					List<RoleMapping> deptList = service.getDeptsList();
					model.addObject("deptList", deptList);
					
					List<Project> sbuList = service5.getSBUsList(null);
					model.addObject("sbuList", sbuList);
					
					List<User> userList = service.getUserFilterList(null);
					model.addObject("userList", userList);
					
					model.addObject("email", user.getEmail_id());
					model.addObject("name", user.getUser_name());
				}
			}else {
				model.addObject("message", "");
				model.setViewName(PageConstants.login);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return model; 
	}
	
	@RequestMapping(value = "/reone/add-new-user-form", method = {RequestMethod.GET,RequestMethod.POST})
	public ModelAndView addUserForm(@ModelAttribute User obj,RedirectAttributes attributes,HttpSession session) {
		boolean flag = false;
		String userId = null;
		String userName = null;
		ModelAndView model = new ModelAndView();
		try {
			model.setViewName(PageConstants.newUserLogin);
			List<RoleMapping> projectsList = service4.getProjectsList(null);
			model.addObject("projectsList", projectsList);
			
			List<RoleMapping> deptList = service.getDeptsList();
			model.addObject("deptList", deptList);
			
			List<Project> sbuList = service5.getSBUsList(null);
			model.addObject("sbuList", sbuList);
			
			List<User> userList = service.getUserFilterList(null);
			model.addObject("userList", userList);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return model;
	}
	
	@RequestMapping(value = "/add-new-user", method = {RequestMethod.GET,RequestMethod.POST})
	public ModelAndView addUserFormMaster(@ModelAttribute User obj,RedirectAttributes attributes,HttpSession session) {
		boolean flag = false;
		String userId = null;
		String userName = null;
		User userDetails = null;
		ModelAndView model = new ModelAndView();
		try {
			model.setViewName("redirect:/login");
			userId = (String) session.getAttribute("USER_ID");
			userName = (String) session.getAttribute("USER_NAME");
			obj.setCreated_by(obj.getUser_id());
			obj.setStatus("Active");
			obj.setBase_role("User");
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd"); 
		    String dt = formatter.format(new Date());
			String endDate = DateForUser.date();
			obj.setEnd_date(endDate);
			obj.setCreated_by(obj.getUser_id());
			obj.setCreated_date(dt);
			flag = service.addUser(obj);
			if(flag == true) {
				//attributes.addFlashAttribute("success", "User Added Succesfully.");
				userDetails = service.validateUser(obj);
				if(!StringUtils.isEmpty(userDetails)) {
					//if((userDetails.getSession_count()) == 0) {
						model.setViewName("redirect:/reone/home");
						User permisions = service.getAllPermissions(userDetails.getBase_role());
						/// USER PERMISISONS
						session.setAttribute("R_ADD", permisions.getP_add());
						session.setAttribute("R_EDIT", permisions.getP_edit());
						session.setAttribute("R_VIEW", permisions.getP_view());
						session.setAttribute("R_APPROVALS", permisions.getP_approvals());
						session.setAttribute("R_REPORTS", permisions.getP_reports());
						session.setAttribute("R_DASHBOARD", permisions.getP_dashboards());
						session.setAttribute("R_AUTO_EMAIL", permisions.getP_auto_email());
						/// USER BASIC SESSION DATA
						session.setAttribute("user", userDetails);
						session.setAttribute("ID", userDetails.getId());
						session.setAttribute("USER_ID", userDetails.getUser_id());
						session.setAttribute("USER_NAME", userDetails.getUser_name());
						session.setAttribute("USER_EMAIL", userDetails.getEmail_id());
						session.setAttribute("BASE_ROLE", userDetails.getBase_role());
						session.setAttribute("USER_IMAGE", obj.getProfileImg());
						session.setAttribute("REPORTING_TO", obj.getReporting_to());
						session.setAttribute("BASE_SBU", userDetails.getBase_sbu());
						session.setAttribute("BASE_PROJECT", userDetails.getProject_name());
						session.setAttribute("BASE_DEPARTMENT", userDetails.getBase_department());
						session.setAttribute("BASE_PROJECT_CODE", userDetails.getBase_project());
						session.setAttribute("CURRENT_PROJECT", obj.getCurrent_project());
						session.setAttribute("SESSION_ID", obj.getUser_session_id());
						List<User> menuList = service.getMenuList();
						session.setAttribute("menuList", menuList);
						attributes.addFlashAttribute("welcome", "welcome "+userDetails.getUser_name());
						attributes.addFlashAttribute("NewUser", "welcome "+userDetails.getUser_name());
					//}else {
						//session.invalidate();
						//model.addObject("multipleLoginFound","Multiple Login found! You have been Logged out from all Devices");
						//model.setViewName(PageConstants.login); 
					//}
				}else{
					model.addObject("invalidEmail",invalidUserName);
					model.setViewName(PageConstants.newUserLogin);
					List<RoleMapping> projectsList = service4.getProjectsList(null);
					model.addObject("projectsList", projectsList);
					
					List<RoleMapping> deptList = service.getDeptsList();
					model.addObject("deptList", deptList);
					
					List<Project> sbuList = service5.getSBUsList(null);
					model.addObject("sbuList", sbuList);
					
					List<User> userList = service.getUserFilterList(null);
					model.addObject("userList", userList);
					
					model.addObject("email", obj.getEmail_id());
				}
				
			}
			else {
				attributes.addFlashAttribute("error","Adding User is failed. Try again.");
			}
		} catch (Exception e) {
			attributes.addFlashAttribute("error","Adding User is failed. Try again.");
			e.printStackTrace();
		}
		return model;
	}
	
	@RequestMapping(value = "/logout", method = {RequestMethod.GET,RequestMethod.POST})
	public ModelAndView logout(HttpSession session,HttpServletRequest request,HttpServletResponse response,RedirectAttributes attributes){
		ModelAndView model = new ModelAndView();
		User user = new User();
		try {
			user.setUser_id((String) session.getAttribute("USER_ID"));
			user.setId((String) session.getAttribute("ID"));
			service.UserLogOutActions(user);
			session.invalidate();
			//model.addObject("success", logOutMessage);
			model.setViewName("redirect:/login");
		} catch (Exception e) {
			logger.fatal("logut() : "+e.getMessage());
		}
		return model;
	}
	
		
}
