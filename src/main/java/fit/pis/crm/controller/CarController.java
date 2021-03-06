package fit.pis.crm.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import fit.pis.crm.data.BrandDAO;
import fit.pis.crm.data.CarDAO;
import fit.pis.crm.data.CarModelDAO;
import fit.pis.crm.data.UserAccDAO;
import fit.pis.crm.model.Brand;
import fit.pis.crm.model.Car;
import fit.pis.crm.model.CarModel;
import fit.pis.crm.model.UserAcc;

@Controller
@RequestMapping(value = "admin/cars")
public class CarController {
	
	@Autowired
	private CarDAO carDAO;
	
	@Autowired
	private UserAccDAO userAccountDAO;
	
	@Autowired
	private BrandDAO brandDAO;
	
	@Autowired
	private CarModelDAO carModelDAO;
	
	private String cars = "cars";
	private String edit = "car_edit";
	
	private Map<Long,String> getBrands() {
		Map<Long,String> map_brands = new LinkedHashMap<Long,String>();
		List<Brand> brands = brandDAO.findAllOrderedByName();
		for (int i = 0; i < brands.size(); i++) {
			map_brands.put(brands.get(i).getId(), brands.get(i).getBrandName());
		}
		
		return map_brands;
	}
	
	private Map<Long,String> getModels() {
		Map<Long,String> map_models = new LinkedHashMap<Long,String>();
		List<CarModel> models = carModelDAO.findAllOrderedByName();
		for (int i = 0; i < models.size(); i++) {
			map_models.put(models.get(i).getId(), models.get(i).getModelName());
		}
		
		return map_models;
	}
	
	public UserAcc getCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String email = auth.getName(); // get logged in user name
		return userAccountDAO.findByEmail(email);
	}
	
	public ModelAndView getModel() {
		ModelAndView model = new ModelAndView();
		model.addObject("currentUser", this.getCurrentUser());
		return model;
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public String showAllCars(Model mod) {
		mod.addAttribute("cars", carDAO.findAllOrderedByBrand());
 		return "cars";
	}
	
	@RequestMapping(value = "/new", method = RequestMethod.GET)
	public ModelAndView newCarGet(@Valid @ModelAttribute("newBrand") Brand brand, BindingResult resultBrand,
			@Valid @ModelAttribute("newModel") CarModel model, BindingResult resultModel) {
		ModelAndView mod = this.getModel();
		mod.setViewName(edit);
		Car newCar = new Car();
		Brand newBrand = new Brand();
		CarModel newModel = new CarModel();
		mod.addObject("newModel", newModel);
		mod.addObject("newBrand", newBrand);
		mod.addObject("models", getModels());
		mod.addObject("brands", getBrands());
		mod.addObject("car", newCar);
		
		if (!resultBrand.hasErrors()) {
			try {
				brandDAO.register(brand);
				mod.setViewName("redirect:/admin/cars/new");
			} catch (UnexpectedRollbackException e) {
				mod.addObject("error", e.getCause().getCause());
				mod.setViewName("redirect:/admin/cars/new");
			}
		} else if (!resultModel.hasErrors()) {
			try {
				carModelDAO.register(model);
				mod.setViewName("redirect:/admin/cars/new");
			} catch (UnexpectedRollbackException e) {
				mod.addObject("error", e.getCause().getCause());
				mod.setViewName("redirect:/admin/cars/new");
			}
		} else {
			mod.setViewName(edit);
			return mod;
		}
		
		return mod;
	}
	
	@RequestMapping(value = "/new", method = RequestMethod.POST)
	public ModelAndView newCarPost(@Valid @ModelAttribute("car") Car car, BindingResult result) {
		ModelAndView mod = this.getModel();
		if (!result.hasErrors()) {
			try {
				carDAO.register(car);
				mod.setViewName("redirect:/admin/cars");
				return mod;
			} catch (UnexpectedRollbackException e) {
				mod.addObject("error", e.getCause().getCause());
				mod.setViewName("admin/cars/new");
				return mod;
			}
		} else {
			mod.setViewName(edit);
			return mod;
		}
	}
	
	@RequestMapping(value = "edit/{id}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable Long id,
			@Valid @ModelAttribute("newBrand") Brand brand, BindingResult resultBrand,
			@Valid @ModelAttribute("newModel") CarModel model, BindingResult resultModel) {
		Car car = carDAO.findById(id);
		ModelAndView mod = this.getModel();
		mod.setViewName(edit);
		Car newCar = new Car();
		Brand newBrand = new Brand();
		CarModel newModel = new CarModel();
		mod.addObject("newModel", newModel);
		mod.addObject("newBrand", newBrand);
		mod.addObject("models", getModels());
		mod.addObject("brands", getBrands());
		mod.addObject("car", car);
		
		if (!resultBrand.hasErrors()) {
			try {
				brandDAO.register(brand);
				mod.setViewName("redirect:/admin/cars/edit/{id}");
			} catch (UnexpectedRollbackException e) {
				mod.addObject("error", e.getCause().getCause());
				mod.setViewName("redirect:/admin/cars/edit/{id}");
			}
		} else if (!resultModel.hasErrors()) {
			try {
				carModelDAO.register(model);
				mod.setViewName("redirect:/admin/cars/edit/{id}");
			} catch (UnexpectedRollbackException e) {
				mod.addObject("error", e.getCause().getCause());
				mod.setViewName("redirect:/admin/cars/edit/{id}");
			}
		} else {
			mod.setViewName(edit);
			return mod;
		}
		
		return mod;
	}
	
	@ModelAttribute("models")
	public Map<Long,String> registerModels() {
	    return getModels();
	}
	
	@ModelAttribute("brands")
	public Map<Long,String> registerBrands() {
	    return getBrands();
	}

	@RequestMapping(value = "/edit/{id}", method = RequestMethod.POST)
	public ModelAndView updateUserAccount(@Valid @ModelAttribute("car") Car car, BindingResult result) {
		ModelAndView mod = this.getModel();
		if (!result.hasErrors()) {
			try {
				carDAO.update(car);
				mod.setViewName("redirect:/admin/cars");
				return mod;
			} catch (UnexpectedRollbackException e) {
				mod.addObject("error", e.getCause().getCause());
				mod.setViewName(edit);
				return mod;
			}
		} else {
			mod.setViewName(edit);
			return mod;
		}
	}
	
	@RequestMapping(value = "{id}", method = RequestMethod.GET)
	public ModelAndView delete(@PathVariable Long id) {
		ModelAndView mod = this.getModel();
		carDAO.deleteById(id);
		mod.setViewName("redirect:/admin/cars");
		return mod;
	}
	
	// Handle modal windows
	@RequestMapping(params = "form_brand") 
    public String addBrand() {
		return "redirect:/admin/cars/new";
	}
	
	@RequestMapping(params = "form_model") 
    public String addModel() {
		return "redirect:/admin/cars/new";
	}

}
