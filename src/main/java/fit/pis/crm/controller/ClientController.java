package fit.pis.crm.controller;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import fit.pis.crm.data.CarDAO;
import fit.pis.crm.data.ClientDAO;
import fit.pis.crm.data.MeetingDAO;
import fit.pis.crm.data.UserAccDAO;
import fit.pis.crm.model.Brand;
import fit.pis.crm.model.Car;
import fit.pis.crm.model.Client;
import fit.pis.crm.model.Meeting;
import fit.pis.crm.model.UserAcc;

@Controller
@RequestMapping(value = "supervisor")
public class ClientController {
	@Autowired
	private UserAccDAO userAccountDAO;
	
	@Autowired
	private ClientDAO clientDAO;
	
	@Autowired
	private CarDAO carDAO;
	
	@Autowired
	private MeetingDAO meetingDAO;
	
	private String clients = "clients";
	private String dashboard = "dashboard";
	private String edit = "client_edit";
	
	public enum Status {
		NEW,
		IN_PROGRESS,
		HAS_CAR,
		ACTIVE
	}
	
	private Map<Status,String> getStatus() {
		Map<Status,String> status = new LinkedHashMap<Status,String>();
		status.put(Status.NEW, "New");
		status.put(Status.IN_PROGRESS,"In progress");
		status.put(Status.ACTIVE,"Active");
		status.put(Status.HAS_CAR,"Has a car");
		
		return status;	
	} 
	
	private Map<Long,String> getManagers() {
		Map<Long,String> map_managers = new LinkedHashMap<Long,String>();
		List<UserAcc> managers = userAccountDAO.findManagers();
		for (int i = 0; i < managers.size(); i++) {
			map_managers.put(managers.get(i).getId(), managers.get(i).getUsername() 
							+ " " + managers.get(i).getSurname());
		}
		return map_managers;
	} 
	
	private Map<Long,Car> getCars() {
		Map<Long,Car> map_cars = new LinkedHashMap<Long,Car>();
		List<Car> cars = carDAO.findAllOrderedByBrand();
		for (int i = 0; i < cars.size(); i++) {
			map_cars.put(cars.get(i).getId(), cars.get(i));
		}
		return map_cars;
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
	
	@RequestMapping(value = "dashboard", method = RequestMethod.GET)
	public ModelAndView showDashboard() throws ParseException {
 		ModelAndView mod = this.getModel();
 		mod.setViewName(dashboard);
 		//Managers with meetings today
 		Calendar cal = Calendar.getInstance();
 		Date todayDat = normalize(cal);
 		List<Meeting> meetingsT = meetingDAO.findToday(todayDat);
 		Integer ms = meetingsT.size();
 		mod.addObject("ms", meetingDAO.findToday(todayDat).size());
 		//clients	
 		mod.addObject("clients", clientDAO.findAllWithoutManager());
 		mod.addObject("lc", clientDAO.calculateAllWithoutManager());
 		// all managers
 		mod.addObject("managers", getManagers());
 		// max manager
 		mod.addObject("mostBusyManager", userAccountDAO.findManagerWithMaxLoad("ROLE_MANAGER"));
 		mod.addObject("maxMeeting", userAccountDAO.findMaxManagerMeetings());
 		// min manager
 	 	mod.addObject("lessBusyManager", userAccountDAO.findManagerWithMinLoad());
 	 	mod.addObject("minMeeting", userAccountDAO.findMinManagerMeetings());
 		
 		return mod;
	}
	
	private Date normalize(Calendar cal) {
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        return cal.getTime();
    }
	
	@RequestMapping(value = "clients", method = RequestMethod.GET)
	public ModelAndView showAllClients() {
 		ModelAndView mod = this.getModel();
 		mod.addObject("clients", clientDAO.findAllOrderedByName());
 		mod.setViewName(clients);
 		return mod;
	}
	
	@RequestMapping(value = "clients/new", method = RequestMethod.GET)
	public ModelAndView newClientGet() {
		ModelAndView mod = this.getModel();
		mod.setViewName(edit);
		Client newClient = new Client();
		mod.addObject("cars", carDAO.findAllOrderedByBrand());
		mod.addObject("status", getStatus());
		mod.addObject("managers", getManagers());
		mod.addObject("client", newClient);
		return mod;
	}
	
	@ModelAttribute("status")
	public Map<Status,String> registerStatus() {
	    return getStatus();
	}
	
	@ModelAttribute("cars")
	public List<Car> registerCars() {
	    return carDAO.findAllOrderedByBrand();
	}
	
	@ModelAttribute("managers")
	public Map<Long,String> registerManagers() {
	    return getManagers();
	}
	
	@RequestMapping(value = "clients/new", method = RequestMethod.POST)
	public ModelAndView newClientPost(@Valid @ModelAttribute("client") Client client, BindingResult result,
			@RequestParam("cars") String car) {
		ModelAndView mod = this.getModel();
		
		if (!(car == null) || !(car.isEmpty())) {
			String[] cars = car.split(",");
			if(cars.length == 3) {
				Car clientCar = carDAO.findByParams(Long.parseLong(cars[0]),Long.parseLong(cars[1]), (String)cars[2]);
				Set<Car> clientCars = new HashSet<Car>();
				clientCars.add(clientCar);
				client.setCars(clientCars);
			}
    		System.out.println(client.getCars());
    		
		}
	
		clientDAO.register(client);
		mod.setViewName("redirect:/supervisor/clients");
		return mod;
		/*if (!result.hasErrors()) {
			try {
				clientDAO.register(client);
				mod.setViewName("redirect:/supervisor/clients");
				return mod;
			} catch (JpaSystemException e) {
				e.printStackTrace();
				mod.addObject("error", e.getCause().getCause());
				mod.setViewName(edit);
				return mod;
			}
		}
		mod.setViewName(edit);
		return mod;*/
	}
	
	@RequestMapping(value = "clients/edit/{id}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable Long id) {
		Client client = clientDAO.findById(id);
		ModelAndView mod = this.getModel();
		mod.setViewName(edit);
		mod.addObject("cars", carDAO.findAllOrderedByBrand());
		mod.addObject("status", getStatus());
		mod.addObject("managers", getManagers());
		mod.addObject("client", client);
		return mod;
	}

	@RequestMapping(value = "clients/edit/{id}", method = RequestMethod.POST)
	public ModelAndView update(@Valid @ModelAttribute("client") Client client, BindingResult result,
			@RequestParam("cars") String car) {
		ModelAndView mod = this.getModel();
		
		if (!(car == null) || !(car.isEmpty())) {
			String[] cars = car.split(",");
			if(cars.length == 3) {
				Car clientCar = carDAO.findByParams(Long.parseLong(cars[0]),Long.parseLong(cars[1]), (String)cars[2]);
				Set<Car> clientCars = new HashSet<Car>();
				clientCars.add(clientCar);
				client.setCars(clientCars);
			}
    		System.out.println(client.getCars());
    		
		}
		
		System.out.println(client.getManagers());
		
	
		clientDAO.update(client);
		mod.setViewName("redirect:/supervisor/clients");
		return mod;
		/*if (!result.hasErrors()) {
			try {
				clientDAO.update(client);;
				mod.setViewName("redirect:/supervisor/clients");
				return mod;
			} catch (JpaSystemException e) {
				e.printStackTrace();
				mod.addObject("error", e.getCause().getCause());
				mod.setViewName(edit);
				return mod;
			}
		}
		mod.setViewName(edit);
		return mod; */
	}
	
	@RequestMapping(value = "clients/{id}", method = RequestMethod.GET)
	public ModelAndView delete(@PathVariable Long id) {
		ModelAndView mod = this.getModel();
		clientDAO.deleteById(id);
		mod.setViewName("redirect:/supervisor/clients");
		return mod;
	}
	
	@InitBinder
	public void initBinder(WebDataBinder binder)
	{
	    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
	    dateFormat.setLenient(false);
	    binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
	}
}
