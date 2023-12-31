package com.iktpreobuka.jobster.services;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.iktpreobuka.jobster.entities.ApplyContactEntity;
import com.iktpreobuka.jobster.entities.CityEntity;
import com.iktpreobuka.jobster.entities.CountryEntity;
import com.iktpreobuka.jobster.entities.CountryRegionEntity;
import com.iktpreobuka.jobster.entities.JobDayHoursEntity;
import com.iktpreobuka.jobster.entities.JobSeekEntity;
import com.iktpreobuka.jobster.entities.UserAccountEntity;
import com.iktpreobuka.jobster.entities.UserEntity;
import com.iktpreobuka.jobster.entities.dto.JobDayHoursDTO;
import com.iktpreobuka.jobster.entities.dto.JobSeekDTO;
import com.iktpreobuka.jobster.entities.dto.JobSeekPutDTO;
import com.iktpreobuka.jobster.enumerations.EDay;
import com.iktpreobuka.jobster.repositories.CityRepository;
import com.iktpreobuka.jobster.repositories.CountryRegionRepository;
import com.iktpreobuka.jobster.repositories.CountryRepository;
import com.iktpreobuka.jobster.repositories.JobDayHoursRepository;
import com.iktpreobuka.jobster.repositories.JobSeekRepository;
import com.iktpreobuka.jobster.repositories.JobTypeRepository;
import com.iktpreobuka.jobster.repositories.UserAccountRepository;

@Service
public class JobSeekDaoImpl implements JobSeekDao {

//	@Autowired
//	private ApplyContactDaoImp applyContactDaoImp;

	@PersistenceContext
	EntityManager em;

	@Autowired
	private CityRepository cityRepository;
	
	@Autowired
	private CountryRepository countryRepository;
	
	@Autowired
	private CountryRegionRepository countryRegionRepository;

	@Autowired
	private CityDao cityDao;

	@Autowired
	private JobTypeRepository jobTypeRepository;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private JobDayHoursRepository jobDayHoursRepository;

	@Autowired
	private JobSeekRepository jobSeekRepository;

	private final Logger logger = (Logger) LoggerFactory.getLogger(this.getClass());

	///////////////////////// POST ///////////////////////////////////////

	@SuppressWarnings({ "unlikely-arg-type", "deprecation" })
	@Override
	public ResponseEntity<?> addNewSeek(@Valid @RequestBody JobSeekDTO seek, Principal principal,
			BindingResult result) {

		logger.info("Starting addNewSeek().---------------------");
		JobSeekEntity newSeek = new JobSeekEntity();

		boolean seekSaved = false;
		boolean dayAndHoursSaved = false;
		List<JobDayHoursEntity> listJobDayHoursEntity = new ArrayList<JobDayHoursEntity>();

		try {

			if (principal.getName() == null) {
				logger.info("Error in geting userName.");
				return new ResponseEntity<String>("Error in geting userName.", HttpStatus.UNAUTHORIZED);
			}
			// Checking does entry data exist

			logger.info("Checking does entry data exist.'");
			if (seek == null) {
				logger.info("Entry data missing.");
				return new ResponseEntity<String>("Entry data missing.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");

			// validating entry

			logger.info("Checking errors.'");
			if (result.hasErrors()) {
				logger.info("Errors exist.");
				return new ResponseEntity<>(createErrorMessage(result), HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");

			// Checking entered data

			if (seek.getCityName() == null || seek.getJobTypeName() == null || seek.getDistanceToJob() == null
					|| seek.getBeginningDate() == null || seek.getEndDate() == null || seek.getPrice() == null
					|| seek.getDetailsLink() == null || seek.getListJobDayHoursDto() == null
					|| seek.getCountryName() == null || seek.getIso2Code() == null || seek.getLongitude() == null
					|| seek.getLatitude() == null) {
				logger.info("Some atributes are null.");
				return new ResponseEntity<String>("Some atributes are null", HttpStatus.BAD_REQUEST);
			}
			if (seek.getCityName().equals(" ") || seek.getJobTypeName().equals(" ")
					|| seek.getDistanceToJob().equals(" ") || seek.getBeginningDate().equals(" ")
					|| seek.getEndDate().equals(" ") || seek.getPrice().equals(" ") || seek.getDetailsLink().equals(" ")
					|| seek.getListJobDayHoursDto().equals(" ") || seek.getCountryName().equals(" ")
					|| seek.getIso2Code().equals(" ") || seek.getLongitude().equals(" ")
					|| seek.getLatitude().equals("") || seek.getCityName().equals("")
					|| seek.getJobTypeName().equals("") || seek.getDistanceToJob().equals("")
					|| seek.getBeginningDate().equals("") || seek.getEndDate().equals("") || seek.getPrice().equals("")
					|| seek.getDetailsLink().equals("") || seek.getListJobDayHoursDto().equals("")
					|| seek.getCountryName().equals("") || seek.getIso2Code().equals("")
					|| seek.getLongitude().equals("") || seek.getLatitude().equals("")) {
				logger.info("Some atributes are blanks.");
				return new ResponseEntity<String>("Some atributes are blanks", HttpStatus.BAD_REQUEST);
			}

			// Checking logged account

			UserAccountEntity loggedAccount = new UserAccountEntity();

			logger.info("Checking userAccount database.");
			if (userAccountRepository.count() == 0) {
				logger.info("UserAccount database empty.");
				return new ResponseEntity<String>("UserAccount database empty.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking does userAccount exist.");
			if (userAccountRepository.findByUsername(principal.getName()) == null) {
				logger.info("UserAccount doesn't exist.");
				return new ResponseEntity<String>("UserAccount doesn't exist.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking is userAccount deleted.");
			if (!(userAccountRepository.findByUsernameAndStatusLike(principal.getName(), 0) == null)) {
				logger.info("UserAccount is deleted.");
				return new ResponseEntity<String>("UserAccount is deleted.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking is userAccount archived.");
			if (!(userAccountRepository.findByUsernameAndStatusLike(principal.getName(), -1) == null)) {
				logger.info("UserAccount is archived.");
				return new ResponseEntity<String>("UserAccount is archived.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking does userAccount have user.");
			if (userAccountRepository.findByUsernameAndStatusLike(principal.getName(), 1).getUser() == null) {
				logger.info("UserAccout doesn't have user.");
				return new ResponseEntity<String>("UserAccout doesn't have user.", HttpStatus.BAD_REQUEST);
			} else {
				logger.info("OK");
				logger.info("UserAccount found.");
				loggedAccount = userAccountRepository.getByUsername(principal.getName());
				logger.info("OK");
			}

			// checking loggedUser

			UserEntity loggedUser = new UserEntity();
			loggedUser = loggedAccount.getUser();

			logger.info("Checking does user have status.");
			if (loggedUser.getStatus() == null) {
				logger.info("User doesn't have status.");
				return new ResponseEntity<String>("User doesn't have status.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking is user deleted.");
			if (loggedUser.getStatus() == 0) {
				logger.info("User is deleted.");
				return new ResponseEntity<String>("User is deleted.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking is user archived.");
			if (loggedUser.getStatus() == -1) {
				logger.info("User is archived.");
				return new ResponseEntity<String>("User is archived.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");

			// checking city

/////////////////// DEO KODA MI NIJE JASAN ///////////// receno mi je samo da ubacim

			CityEntity city = new CityEntity();

			logger.info("Checking database for city.");
			if ((seek.getCityName() != null) || (!seek.getCityName().equals("")) || (!seek.getCityName().equals(" "))) {
				if (((cityRepository.count()) == 0) || (cityRepository.getByCityName(seek.getCityName()) == null)) {
					//////// ODAVDE mi nije jasno
					logger.info("Creating city.");
					city = cityDao.addNewCityWithLoggedUser(seek.getCityName(), seek.getLongitude(), seek.getLatitude(),
							seek.getCountryRegionName(), seek.getCountryName(), seek.getIso2Code(), loggedUser);
					///////// DOVDE
				} else {
					city = cityRepository.getByCityName(seek.getCityName());
				}
			}
			logger.info("OK");

			// checking jobType

			if ((seek.getJobTypeName() != null) || (!seek.getJobTypeName().equals(""))
					|| (!seek.getJobTypeName().equals(" "))) {
				logger.info("Checking database for jobType.");
				if ((jobTypeRepository.count() == 0)
						|| (jobTypeRepository.getByJobTypeName(seek.getJobTypeName())) == null) {
					logger.info("JobType doesn't exist.");
					return new ResponseEntity<String>("JobType doesn't exist.", HttpStatus.BAD_REQUEST);
				}
			}

			// checking size of list of checked days, and are there duplicate days, and

			List<JobDayHoursDTO> listJobDayHoursPostDto = new ArrayList<JobDayHoursDTO>();
			listJobDayHoursPostDto = seek.getListJobDayHoursDto();

			if (listJobDayHoursPostDto.size() > 0) {
				logger.info("Checking size of list of checked days and are there duplicate days.");
				if (listJobDayHoursPostDto.size() > 7) {
					logger.info("List can contain 7 elements max.");
					return new ResponseEntity<String>("List can contain 7 elements max.", HttpStatus.BAD_REQUEST);
				}

				Integer monday = 0;
				Integer tuesday = 0;
				Integer wednesday = 0;
				Integer thursday = 0;
				Integer friday = 0;
				Integer saturday = 0;
				Integer sunday = 0;

				for (JobDayHoursDTO i : listJobDayHoursPostDto) {
//					if (i.getFlexibileHours() == true && i.getIsMinMax() == true) {
//						return new ResponseEntity<String>("You need to decide between 'flexibile hours' and 'MinMax'.",
//								HttpStatus.BAD_REQUEST);
//					}
//					if (i.getFlexibileHours() == false && i.getIsMinMax() == false) {
//						return new ResponseEntity<String>("You need to decide between 'flexibile hours' and 'MinMax'.",
//								HttpStatus.BAD_REQUEST);
//					}
					if (i.getDay().equals(EDay.DAY_MONDAY)) {
						monday++;
					}
					if (i.getDay().equals(EDay.DAY_TUESDAY)) {
						tuesday++;
					}
					if (i.getDay().equals(EDay.DAY_WEDNESDAY)) {
						wednesday++;
					}
					if (i.getDay().equals(EDay.DAY_THURSDAY)) {
						thursday++;
					}
					if (i.getDay().equals(EDay.DAY_FRIDAY)) {
						friday++;
					}
					if (i.getDay().equals(EDay.DAY_SATURDAY)) {
						saturday++;
					}
					if (i.getDay().equals(EDay.DAY_SUNDAY)) {
						sunday++;
					}
				}
				if (monday > 1 || tuesday > 1 || wednesday > 1 || thursday > 1 || friday > 1 || saturday > 1
						|| sunday > 1) {
					logger.info("You have duplicate days.");
					return new ResponseEntity<String>("You have duplicate days.", HttpStatus.BAD_REQUEST);
				}
			}
			logger.info("OK");
			
			// Checking does dates match
			
	        Date begining = seek.getBeginningDate();
	        Date end = seek.getEndDate();
	        Date created = Calendar.getInstance().getTime();
	        
	        Calendar c = new GregorianCalendar();
	        c.setTime(created);
	        c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            created = c.getTime();
			
			logger.info("Checking does dates match.");
			if(begining.compareTo(end) >= 0) {
				logger.info("Beginning date must be younger than end date.");
				return new ResponseEntity<String>("Beginning date must be younger than end date.", HttpStatus.BAD_REQUEST);
			}
			if(begining.compareTo(created) < 0) {
				logger.info("Beginning date must be todays or future date.");
				return new ResponseEntity<String>("Beginning date must be todays or future date.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			
			// Mapping atributs

			logger.info("Mapping atributs for JobSeek.");
			newSeek.setEmployee(loggedUser);
			newSeek.setCity(city);
			newSeek.setType(jobTypeRepository.getByJobTypeName(seek.getJobTypeName()));
			newSeek.setDistanceToJob(seek.getDistanceToJob());
			newSeek.setBeginningDate(seek.getBeginningDate());
			newSeek.setEndDate(seek.getEndDate());
			newSeek.setDateCreated(Calendar.getInstance().getTime());
			newSeek.setFlexibileDates(seek.getFlexibileDates());
			newSeek.setPrice(seek.getPrice());
			newSeek.setDetailsLink(seek.getDetailsLink());
			newSeek.setFlexibileDays(seek.getFlexibileDays());
			newSeek.setCounterSeek(false);
			newSeek.setStatusActive();
			newSeek.setExpired(false);
			newSeek.setVersion(1);
			newSeek.setCreatedById(loggedUser.getId());
			logger.info("Saveing JobSeek to database.");
			newSeek = jobSeekRepository.save(newSeek);
			logger.info("OK");
			seekSaved = true;

			logger.info("Mapping days and hours.");
			Integer numberOfDays = 0;
			for (JobDayHoursDTO i : listJobDayHoursPostDto) {
				JobDayHoursEntity newDayAndHours = new JobDayHoursEntity();
				newDayAndHours.setDay(i.getDay());
				newDayAndHours.setFromHour(i.getFromHour());
				newDayAndHours.setToHour(i.getToHour());
				newDayAndHours.setFlexibileHours(i.getFlexibileHours());
				newDayAndHours.setIsMinMax(i.getIsMinMax());
				newDayAndHours.setStatusActive();
				newDayAndHours.setVersion(1);
				newDayAndHours.setSeek(newSeek);
				logger.info("Saveing day and hours for day" + i.getDay());
				jobDayHoursRepository.save(newDayAndHours);
				listJobDayHoursEntity.add(newDayAndHours);
				logger.info("OK");
				dayAndHoursSaved = true;
				numberOfDays++;
			}
			logger.info("There is " + numberOfDays + " day(s) added.");
			logger.info("Days and hours mapped.");
			logger.info("OK");
			logger.info("Atributs for JobSeek mapped.");

		} catch (Exception e) {
			logger.info("Error occurred and data that has been previously added needs to be removed from database.");
			if (seekSaved == true) {
				if (jobSeekRepository.findById(newSeek.getId()) != null) {
					jobSeekRepository.delete(newSeek);
					logger.info("Job Seek that has been previously added removed from database.");
				}
			}
			if (dayAndHoursSaved == true) {
				if (!(listJobDayHoursEntity.isEmpty())) {
					for (JobDayHoursEntity i : listJobDayHoursEntity) {
						if (jobDayHoursRepository.findById(i.getId()) != null) {
							jobDayHoursRepository.delete(i);
							logger.info("JobDayHours that has been previously added removed from database.");
						}
					}
				}
			}
			return new ResponseEntity<String>("Error occurrred.------------------- " + e.getMessage() + " " + e,
					HttpStatus.BAD_REQUEST);
		}

		logger.info("Returning new jobSeek.---------------");
		return new ResponseEntity<JobSeekEntity>(newSeek, HttpStatus.OK);
	}
	
///////////////////////////// PUT /////////////////////////////
	
	@Override
	public ResponseEntity<?> modifySeek(@Valid JobSeekPutDTO seek, Integer seekId, Principal principal,
			BindingResult result) {
		
		logger.info("Starting modifySeek().---------------------");

		JobSeekEntity seekForModify = new JobSeekEntity();

		JobSeekEntity copyOfOriginalJobSeekEntity = new JobSeekEntity();

		boolean seekSaved = false;

		try {

			if (principal.getName() == null) {
				logger.info("Error in geting userName.");
				return new ResponseEntity<String>("Error in geting userName.", HttpStatus.UNAUTHORIZED);
			}

// Checking does entry data exist

			logger.info("Checking does entry data exist.'");
			if (seek == null) {
				logger.info("Entry data missing.");
				return new ResponseEntity<String>("Entry data missing.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");

// validating entry

			logger.info("Checking errors.'");
			if (result.hasErrors()) {
				logger.info("Errors exist.");
				return new ResponseEntity<>(createErrorMessage(result), HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");

// Checking entered data

			if (seek.getDetailsLink() == null) {
				logger.info("DetailsLink are null.");
				return new ResponseEntity<String>("DetailsLink are null.", HttpStatus.BAD_REQUEST);
			}
			if (seek.getDetailsLink().equals(" ") || seek.getDetailsLink().equals("")) {
				logger.info("DetailsLink are blanks.");
				return new ResponseEntity<String>("Some atributes are blanks", HttpStatus.BAD_REQUEST);
			}

// Checking logged account

			UserAccountEntity loggedAccount = new UserAccountEntity();

			logger.info("Checking userAccount database.");
			if (userAccountRepository.count() == 0) {
				logger.info("UserAccount database empty.");
				return new ResponseEntity<String>("UserAccount database empty.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking does userAccount exist.");
			if (userAccountRepository.findByUsername(principal.getName()) == null) {
				logger.info("UserAccount doesn't exist.");
				return new ResponseEntity<String>("UserAccount doesn't exist.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking is userAccount deleted.");
			if (!(userAccountRepository.findByUsernameAndStatusLike(principal.getName(), 0) == null)) {
				logger.info("UserAccount is deleted.");
				return new ResponseEntity<String>("UserAccount is deleted.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking is userAccount archived.");
			if (!(userAccountRepository.findByUsernameAndStatusLike(principal.getName(), -1) == null)) {
				logger.info("UserAccount is archived.");
				return new ResponseEntity<String>("UserAccount is archived.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking does userAccount have user.");
			if (userAccountRepository.findByUsernameAndStatusLike(principal.getName(), 1).getUser() == null) {
				logger.info("UserAccout doesn't have user.");
				return new ResponseEntity<String>("UserAccout doesn't have user.", HttpStatus.BAD_REQUEST);
			} else {
				logger.info("OK");
				logger.info("UserAccount found.");
				loggedAccount = userAccountRepository.getByUsername(principal.getName());
				logger.info("OK");
			}

// checking loggedUser

			UserEntity loggedUser = new UserEntity();
			loggedUser = loggedAccount.getUser();

			logger.info("Checking does user have status.");
			if (loggedUser.getStatus() == null) {
				logger.info("User doesn't have status.");
				return new ResponseEntity<String>("User doesn't have status.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking is user deleted.");
			if (loggedUser.getStatus() == 0) {
				logger.info("User is deleted.");
				return new ResponseEntity<String>("User is deleted.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking is user archived.");
			if (loggedUser.getStatus() == -1) {
				logger.info("User is archived.");
				return new ResponseEntity<String>("User is archived.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");

// checking seekForModify

			logger.info("Looking for jobSeek you want to change.");

			logger.info("Checking jobSeek database.");
			if (jobSeekRepository.count() == 0) {
				logger.info("JobSeek database empty.");
				return new ResponseEntity<String>("JobSeek database empty.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking does jobSeek exist.");
			if (jobSeekRepository.findById(seekId) == null) {
				logger.info("JobSeek doesn't exist.");
				return new ResponseEntity<String>("JobSeek doesn't exist.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking is jobSeek deleted.");
			if (!(jobSeekRepository.findByIdAndStatusLike(seekId, 0) == null)) {
				logger.info("JobSeek is deleted.");
				return new ResponseEntity<String>("JobSeek is deleted.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking is jobSeek archived.");
			if (!(jobSeekRepository.findByIdAndStatusLike(seekId, -1) == null)) {
				logger.info("JobSeek is archived.");
				return new ResponseEntity<String>("JobSeek is archived.", HttpStatus.BAD_REQUEST);
			} else {
				logger.info("OK");
				seekForModify = jobSeekRepository.findById(seekId).orElse(null);
				if (seekForModify == null) {
					logger.info("JobSeek that you asked for doesn't exist.");
					return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.",
							HttpStatus.BAD_REQUEST);
				}
			}

// Making copy of seekForModify

			copyOfOriginalJobSeekEntity = seekForModify;

// Mapping atributs

			logger.info("Checking are details changed.");
			if (!(seek.getDetailsLink().equals(seekForModify.getDetailsLink()))) {
				logger.info("Mapping atributs.");
				seekForModify.setDetailsLink(seek.getDetailsLink());
				logger.info("Setting update details.");
				seekForModify.setUpdatedById(loggedUser.getId());
				seekForModify.setDateUpdated(Calendar.getInstance().getTime());
				logger.info("Update details set.");
				logger.info("Details changed");
				logger.info("Saveing JobSeek.");
				jobSeekRepository.save(seekForModify);
				seekSaved = true;
				logger.info("Atributs mapped.");
			}
			
		} catch (Exception e) {
			logger.info("Error occurred and data that has been previously added needs to be removed from database.");
			if (seekSaved == true) {
				if (jobSeekRepository.findById(seekForModify.getId()) != null) {
					seekForModify = copyOfOriginalJobSeekEntity;
					jobSeekRepository.save(seekForModify);
					logger.info("Job Seek that has been previously modified has been returned to previus state.");
				}
			}
			return new ResponseEntity<String>("Error occurrred.------------------- " + e.getMessage() + " " + e,
					HttpStatus.BAD_REQUEST);

		}
		logger.info("Returning new jobSeek.");
		return new ResponseEntity<JobSeekEntity>(seekForModify, HttpStatus.OK);
	}
		

	

///////////////////////////// PUT STARA METODA/////////////////////////////

	@SuppressWarnings("unlikely-arg-type")
	@Override
	public ResponseEntity<?> modifySeekStaraMetoda(@Valid @RequestBody JobSeekDTO seek, @PathVariable Integer seekId,
			Principal principal, BindingResult result) {

		logger.info("Starting modifySeek().---------------------");

		JobSeekEntity seekForModify = new JobSeekEntity();

		JobDayHoursEntity newDayToCreate = new JobDayHoursEntity();

		JobSeekEntity copyOfOriginalJobSeekEntity = new JobSeekEntity();

		List<JobDayHoursEntity> copyOfOriginalListOfJobDayHoursEntitys = new ArrayList<JobDayHoursEntity>();

		boolean seekSaved = false;
		boolean dayAndHoursSaved = false;

		try {

			if (principal.getName() == null) {
				logger.info("Error in geting userName.");
				return new ResponseEntity<String>("Error in geting userName.", HttpStatus.UNAUTHORIZED);
			}

// Checking does entry data exist

			logger.info("Checking does entry data exist.'");
			if (seek == null) {
				logger.info("Entry data missing.");
				return new ResponseEntity<String>("Entry data missing.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");

// validating entry

			logger.info("Checking errors.'");
			if (result.hasErrors()) {
				logger.info("Errors exist.");
				return new ResponseEntity<>(createErrorMessage(result), HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");

// Checking entered data

			if (seek.getCityName() == null || seek.getJobTypeName() == null || seek.getDistanceToJob() == null
					|| seek.getBeginningDate() == null || seek.getEndDate() == null || seek.getPrice() == null
					|| seek.getDetailsLink() == null || seek.getListJobDayHoursDto() == null
					|| seek.getCountryName() == null || seek.getIso2Code() == null || seek.getLongitude() == null
					|| seek.getLatitude() == null) {
				logger.info("Some atributes are null.");
				return new ResponseEntity<String>("Some atributes are null", HttpStatus.BAD_REQUEST);
			}
			if (seek.getCityName().equals(" ") || seek.getJobTypeName().equals(" ")
					|| seek.getDistanceToJob().equals(" ") || seek.getBeginningDate().equals(" ")
					|| seek.getEndDate().equals(" ") || seek.getPrice().equals(" ") || seek.getDetailsLink().equals(" ")
					|| seek.getListJobDayHoursDto().equals(" ") || seek.getCountryName().equals(" ")
					|| seek.getIso2Code().equals(" ") || seek.getLongitude().equals(" ")
					|| seek.getLatitude().equals("") || seek.getCityName().equals("")
					|| seek.getJobTypeName().equals("") || seek.getDistanceToJob().equals("")
					|| seek.getBeginningDate().equals("") || seek.getEndDate().equals("") || seek.getPrice().equals("")
					|| seek.getDetailsLink().equals("") || seek.getListJobDayHoursDto().equals("")
					|| seek.getCountryName().equals("") || seek.getIso2Code().equals("")
					|| seek.getLongitude().equals("") || seek.getLatitude().equals("")) {
				logger.info("Some atributes are blanks.");
				return new ResponseEntity<String>("Some atributes are blanks", HttpStatus.BAD_REQUEST);
			}

// Checking logged account

			UserAccountEntity loggedAccount = new UserAccountEntity();

			logger.info("Checking userAccount database.");
			if (userAccountRepository.count() == 0) {
				logger.info("UserAccount database empty.");
				return new ResponseEntity<String>("UserAccount database empty.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking does userAccount exist.");
			if (userAccountRepository.findByUsername(principal.getName()) == null) {
				logger.info("UserAccount doesn't exist.");
				return new ResponseEntity<String>("UserAccount doesn't exist.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking is userAccount deleted.");
			if (!(userAccountRepository.findByUsernameAndStatusLike(principal.getName(), 0) == null)) {
				logger.info("UserAccount is deleted.");
				return new ResponseEntity<String>("UserAccount is deleted.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking is userAccount archived.");
			if (!(userAccountRepository.findByUsernameAndStatusLike(principal.getName(), -1) == null)) {
				logger.info("UserAccount is archived.");
				return new ResponseEntity<String>("UserAccount is archived.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking does userAccount have user.");
			if (userAccountRepository.findByUsernameAndStatusLike(principal.getName(), 1).getUser() == null) {
				logger.info("UserAccout doesn't have user.");
				return new ResponseEntity<String>("UserAccout doesn't have user.", HttpStatus.BAD_REQUEST);
			} else {
				logger.info("OK");
				logger.info("UserAccount found.");
				loggedAccount = userAccountRepository.getByUsername(principal.getName());
				logger.info("OK");
			}

// checking loggedUser

			UserEntity loggedUser = new UserEntity();
			loggedUser = loggedAccount.getUser();

			logger.info("Checking does user have status.");
			if (loggedUser.getStatus() == null) {
				logger.info("User doesn't have status.");
				return new ResponseEntity<String>("User doesn't have status.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking is user deleted.");
			if (loggedUser.getStatus() == 0) {
				logger.info("User is deleted.");
				return new ResponseEntity<String>("User is deleted.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking is user archived.");
			if (loggedUser.getStatus() == -1) {
				logger.info("User is archived.");
				return new ResponseEntity<String>("User is archived.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");

// checking seekForModify

			logger.info("Looking for jobSeek you want to change.");

			logger.info("Checking jobSeek database.");
			if (jobSeekRepository.count() == 0) {
				logger.info("JobSeek database empty.");
				return new ResponseEntity<String>("JobSeek database empty.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking does jobSeek exist.");
			if (jobSeekRepository.findById(seekId) == null) {
				logger.info("JobSeek doesn't exist.");
				return new ResponseEntity<String>("JobSeek doesn't exist.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking is jobSeek deleted.");
			if (!(jobSeekRepository.findByIdAndStatusLike(seekId, 0) == null)) {
				logger.info("JobSeek is deleted.");
				return new ResponseEntity<String>("JobSeek is deleted.", HttpStatus.BAD_REQUEST);
			}
			logger.info("OK");
			logger.info("Checking is jobSeek archived.");
			if (!(jobSeekRepository.findByIdAndStatusLike(seekId, -1) == null)) {
				logger.info("JobSeek is archived.");
				return new ResponseEntity<String>("JobSeek is archived.", HttpStatus.BAD_REQUEST);
			} else {
				logger.info("OK");
				seekForModify = jobSeekRepository.findById(seekId).orElse(null);
				if (seekForModify == null) {
					logger.info("JobSeek that you asked for doesn't exist.");
					return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.",
							HttpStatus.BAD_REQUEST);
				}
			}

// checking city

			logger.info("Looking for City you want to change.");

/////////////////// DEO KODA MI NIJE JASAN ///////////// receno mi je samo da ubacim

			CityEntity city = new CityEntity();

			logger.info("Checking database for city.");
			if ((seek.getCityName() != null) || (!seek.getCityName().equals("")) || (!seek.getCityName().equals(" "))) {
				if (((cityRepository.count()) == 0) || (cityRepository.getByCityName(seek.getCityName()) == null)) {
//////// ODAVDE mi nije jasno
					logger.info("Creating city.");
					city = cityDao.addNewCityWithLoggedUser(seek.getCityName(), seek.getLongitude(), seek.getLatitude(),
							seek.getCountryRegionName(), seek.getCountryName(), seek.getIso2Code(), loggedUser);
///////// DOVDE
				} else {
					city = cityRepository.getByCityName(seek.getCityName());
				}
			}
			logger.info("OK");

// checking jobType

			if ((seek.getJobTypeName() != null) || (!seek.getJobTypeName().equals(""))
					|| (!seek.getJobTypeName().equals(" "))) {
				logger.info("Checking database for jobType.");
				if ((jobTypeRepository.count() == 0)
						|| (jobTypeRepository.getByJobTypeName(seek.getJobTypeName())) == null) {
					logger.info("JobType doesn't exist.");
					return new ResponseEntity<String>("JobType doesn't exist.", HttpStatus.BAD_REQUEST);
				}
			}
			logger.info("OK");

// Making copy of seekForModify and list of JobDayHours that seekForModify has

			copyOfOriginalJobSeekEntity = seekForModify;
			copyOfOriginalListOfJobDayHoursEntitys = seekForModify.getDaysAndHours();

// Mapping atributs

			logger.info("Mapping atributs.");
//seekForModify.setEmployee(loggedUser);

			logger.info("Checking is city changed.");
			if (((seek.getCityName() != null) || (!seek.getCityName().equals("")) || (!seek.getCityName().equals(" ")))
					&& (cityRepository.getByCityName(seek.getCityName()) != seekForModify.getCity())) {
				seekForModify.setCity(city);
				logger.info("City changed.");
			}
			logger.info("Checking is job type name changed.");
			if (((seek.getJobTypeName() != null) || (!seek.getJobTypeName().equals(""))
					|| (!seek.getJobTypeName().equals(" ")))
					&& (!seek.getJobTypeName().equalsIgnoreCase(seekForModify.getType().getJobTypeName()))) {
				seekForModify.setType(jobTypeRepository.getByJobTypeName(seek.getJobTypeName()));
				logger.info("Job type name changed.");
			}
			logger.info("Checking is distance to job changed.");
			if (((seek.getDistanceToJob() != null) || (!seek.getDistanceToJob().equals(""))
					|| (!seek.getDistanceToJob().equals(" ")))
					&& (seek.getDistanceToJob() != seekForModify.getDistanceToJob())) {
				seekForModify.setDistanceToJob(seek.getDistanceToJob());
				logger.info("Distance to job changed.");
			}
///////////////ovo popraviti (registruje promenu i kada nema promene izmedju JSON-a koje se koristio za POST i JSON-a koji se koristio za PUT)
			logger.info("Checking is beginning date changed.");
			if (((seek.getBeginningDate() != null) || (!seek.getBeginningDate().equals(""))
					|| (!seek.getBeginningDate().equals(" ")))
					&& (seek.getBeginningDate() != seekForModify.getBeginningDate())) {
				seekForModify.setBeginningDate(seek.getBeginningDate());
				logger.info("Beginning date changed.");
			}
///////////////ovo popraviti (registruje promenu i kada nema promene izmedju JSON-a koje se koristio za POST i JSON-a koji se koristio za PUT)
			logger.info("Checking is end date changed.");
			if (((seek.getEndDate() != null) || (!seek.getEndDate().equals("")) || (!seek.getEndDate().equals(" ")))
					&& (seek.getEndDate() != seekForModify.getEndDate())) {
				seekForModify.setEndDate(seek.getEndDate());
				logger.info("End date changed.");
			}
			logger.info("Checking is flexibility for dates changed.");
			if (((seek.getFlexibileDates() != null) || (!seek.getFlexibileDates().equals(""))
					|| (!seek.getFlexibileDates().equals(" ")))
					&& (seek.getFlexibileDates() != seekForModify.getFlexibileDates())) {
				seekForModify.setFlexibileDates(seek.getFlexibileDates());
				logger.info("Flexibility for dates changed.");
			}
///////////////ovo popraviti (registruje promenu i kada nema promene izmedju JSON-a koje se koristio za POST i JSON-a koji se koristio za PUT)
			logger.info("Checking is price changed.");
			if (((seek.getPrice() != null) || (!seek.getPrice().equals("")) || (!seek.getPrice().equals(" ")))
					&& (seek.getPrice() != seekForModify.getPrice())) {
				seekForModify.setPrice(seek.getPrice());
				logger.info("Price changed.");
			}
///////////////ovo popraviti (registruje promenu i kada nema promene izmedju JSON-a koje se koristio za POST i JSON-a koji se koristio za PUT)
			logger.info("Checking are details changed.");
			if (((seek.getDetailsLink() != null) || (!seek.getDetailsLink().equals(""))
					|| (!seek.getDetailsLink().equals(" ")))
					&& (seek.getDetailsLink().equals(seekForModify.getDetailsLink()))) {
				seekForModify.setDetailsLink(seek.getDetailsLink());
				logger.info("Details changed");
			}
			logger.info("Checking is flexibility for days changed.");
			if (((seek.getFlexibileDays() != null) || (!seek.getFlexibileDays().equals(""))
					|| (!seek.getFlexibileDays().equals(" ")))
					&& (seek.getFlexibileDays() != seekForModify.getFlexibileDays())) {
				seekForModify.setFlexibileDays(seek.getFlexibileDays());
				logger.info("Flexibility for days changed.");
			}
			logger.info("Setting update details.");
			seekForModify.setUpdatedById(loggedUser.getId());
			seekForModify.setDateCreated(Calendar.getInstance().getTime());
			logger.info("Update details set.");

			///////////// Checking NEW list for jobDayHours

			// --------------------- NOVA LISTA -------------------------------
			List<JobDayHoursDTO> listJobDayHoursDto = new ArrayList<JobDayHoursDTO>();

			listJobDayHoursDto = seek.getListJobDayHoursDto();

			logger.info("Checking is list for days and hours empty.");
			if (listJobDayHoursDto.size() > 0) {
				logger.info("List has elements.");

				logger.info("Checking number of elements of list.");
				if (listJobDayHoursDto.size() > 7) {
					logger.info("You can choose 7 days max.");
					return new ResponseEntity<String>("You can choose 7 days max.", HttpStatus.BAD_REQUEST);
				}
				logger.info("OK.");

				Integer monday = 0;
				Integer tuesday = 0;
				Integer wednesday = 0;
				Integer thursday = 0;
				Integer friday = 0;
				Integer saturday = 0;
				Integer sunday = 0;

				logger.info("Checking input data for daysAndHours.");
				for (JobDayHoursDTO i : listJobDayHoursDto) {
					if ((i.getDay() == null) || (i.getDay().equals("")) || (i.getDay().equals(" "))) {
						logger.info("Missing data. You need to put 'day'.");
						return new ResponseEntity<String>("You need to put 'day'.", HttpStatus.BAD_REQUEST);
					}
					if ((i.getFromHour() == null) || (i.getFromHour().equals("")) || (i.getFromHour().equals(" "))) {
						logger.info("Missing data. You need to put 'from hour'.");
						return new ResponseEntity<String>("You need to put 'from hour'.", HttpStatus.BAD_REQUEST);
					}
					if ((i.getToHour() == null) || (i.getToHour().equals("")) || (i.getToHour().equals(" "))) {
						logger.info("Missing data. You need to put 'to hour'.");
						return new ResponseEntity<String>("You need to put 'to hour'.", HttpStatus.BAD_REQUEST);
					}
//					if (i.getFlexibileHours() == null && i.getIsMinMax() == null) {
//						return new ResponseEntity<String>("You need to decide between 'flexibile hours' and 'MinMax'.",
//								HttpStatus.BAD_REQUEST);
//					}
//					if (i.getFlexibileHours() == true && i.getIsMinMax() == true) {
//						return new ResponseEntity<String>("You need to decide between 'flexibile hours' and 'MinMax'.",
//								HttpStatus.BAD_REQUEST);
//					}
//					if (i.getFlexibileHours() == false && i.getIsMinMax() == false) {
//						return new ResponseEntity<String>("You need to decide between 'flexibile hours' and 'MinMax'.",
//								HttpStatus.BAD_REQUEST);
//					}
					if (i.getDay().equals(EDay.DAY_MONDAY)) {
						monday++;
					}
					if (i.getDay().equals(EDay.DAY_TUESDAY)) {
						tuesday++;
					}
					if (i.getDay().equals(EDay.DAY_WEDNESDAY)) {
						wednesday++;
					}
					if (i.getDay().equals(EDay.DAY_THURSDAY)) {
						thursday++;
					}
					if (i.getDay().equals(EDay.DAY_FRIDAY)) {
						friday++;
					}
					if (i.getDay().equals(EDay.DAY_SATURDAY)) {
						saturday++;
					}
					if (i.getDay().equals(EDay.DAY_SUNDAY)) {
						sunday++;
					}
				}
				if (monday > 1 || tuesday > 1 || wednesday > 1 || thursday > 1 || friday > 1 || saturday > 1
						|| sunday > 1) {
					logger.info("You have duplicate days.");
					return new ResponseEntity<String>("You have duplicate days.", HttpStatus.BAD_REQUEST);
				}

				// --------------------- STARA LISTA ------------------------------
				// provera da li je doslo do izmene sati ili drugih atributa za neki dan
				// ako jeste na stari entitet mapirati nove atribute
				// takodje provera i da li treba vec neki postojeci dan staviti da bude
				// neaktivan

				List<JobDayHoursEntity> oldListJobDayHours = new ArrayList<JobDayHoursEntity>();
				oldListJobDayHours = seekForModify.getDaysAndHours();

				boolean missing;
				for (JobDayHoursEntity i : oldListJobDayHours) {
					missing = true;
					for (JobDayHoursDTO j : listJobDayHoursDto) {
						if (i.getDay() == j.getDay()) {
							missing = false;
							i.setFromHour(j.getFromHour());
							i.setToHour(j.getToHour());
							i.setFlexibileHours(j.getFlexibileHours());
							i.setIsMinMax(j.getIsMinMax());
							i.setStatusActive();
							logger.info("Saving corected day." + i.getDay());
							jobDayHoursRepository.save(i);
							dayAndHoursSaved = true;
							logger.info("Corected day saved.");
						}
					}
					if (missing == true) {
						logger.info("Deleting unwanted day." + i.getDay());
						i.setStatusInactive();
						jobDayHoursRepository.save(i);
						dayAndHoursSaved = true;
						logger.info("Unwanted day deleted.");
					}
				}

				// --------------------- NOVA LISTA ------------------------------
				// provera da li ima neki novi dan koji treba napraviti
				// i ako ima pravljenje novog dana

				Integer difrenDayCount;
				Integer allDaysCount;
				for (JobDayHoursDTO z : listJobDayHoursDto) {
					difrenDayCount = 0;
					allDaysCount = 0;
					for (JobDayHoursEntity y : oldListJobDayHours) {
						allDaysCount++;
						if (z.getDay() != y.getDay()) {
							difrenDayCount++;
						}
					}
					if (difrenDayCount == allDaysCount) {
						newDayToCreate.setDay(z.getDay());
						newDayToCreate.setFromHour(z.getFromHour());
						newDayToCreate.setToHour(z.getToHour());
						newDayToCreate.setFlexibileHours(z.getFlexibileHours());
						newDayToCreate.setIsMinMax(z.getIsMinMax());
						newDayToCreate.setStatusActive();
						newDayToCreate.setSeek(seekForModify);
						logger.info("Saving new created day." + z.getDay());
						jobDayHoursRepository.save(newDayToCreate);
						dayAndHoursSaved = true;
						logger.info("New created day saved.");
					}
				}
				logger.info("Saveing JobSeek.");
				jobSeekRepository.save(seekForModify);
				seekSaved = true;
				logger.info("Atributs mapped.");
			}
		} catch (Exception e) {
			logger.info("Error occurred and data that has been previously added needs to be removed from database.");
			if (dayAndHoursSaved == true) {
				logger.info("Correcting change JobDaysAndHours.");
				// ovaj array bi mozda mogao praviti problem
				Integer[] ids = new Integer[6];
				Integer position = 0;
				if (!(seekForModify.getDaysAndHours().isEmpty())) {
					for (JobDayHoursEntity i : seekForModify.getDaysAndHours()) {
						for (JobDayHoursEntity j : copyOfOriginalListOfJobDayHoursEntitys) {
							if (i.getId().equals(j.getId())) {
								i = j;
								jobDayHoursRepository.save(i);
								logger.info(position + 1 + " jobDayAndHours has been returned to previous state");
								ids[position] = i.getId();
								position++;
							}
						}
					}
					logger.info("Deleting added JobDaysAndHours.");
					JobDayHoursEntity jobDayHoursForDelete = new JobDayHoursEntity();
					for (Integer x = 0; x <= ids.length; x++) {
						Integer countForCurcles = 0;
						Integer countDifrentIds = 0;
						for (JobDayHoursEntity y : seekForModify.getDaysAndHours()) {
							jobDayHoursForDelete = y;
							countForCurcles++;
							// ovde bi mozda array mogao da napravi problem
							if (ids[x] != null && ids[x] != y.getId()) {
								countDifrentIds++;
							}
						}
						if (countForCurcles == countDifrentIds) {
							jobDayHoursRepository.delete(jobDayHoursForDelete);
							logger.info(x + 1 + " jobDayAndHours has been deleted");
						}
					}
				}
			}
			if (seekSaved == true) {
				if (jobSeekRepository.findById(seekForModify.getId()) != null) {
					seekForModify = copyOfOriginalJobSeekEntity;
					jobSeekRepository.save(seekForModify);
					logger.info("Job Seek that has been previously modified has been returned to previus state.");
				}
			}
			return new ResponseEntity<String>("Error occurrred.------------------- " + e.getMessage() + " " + e,
					HttpStatus.BAD_REQUEST);

		}
		logger.info("Returning new jobSeek.");
		return new ResponseEntity<JobSeekEntity>(seekForModify, HttpStatus.OK);
	}

/////////////////////////////////////////////GET ALL /////////////////////////////////////////

	@Override
	public ResponseEntity<?> getAll() {
		try {
			logger.info("Checking database for JobSeek.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.NOT_FOUND);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		List<JobSeekEntity> list = (List<JobSeekEntity>) jobSeekRepository.findAll();
		logger.info("Returning result.");
		return new ResponseEntity<List<JobSeekEntity>>(list, HttpStatus.OK);
	}

////////////////////////////////////////////GET BY ID///////////////////////////////////////////

	@Override
	public ResponseEntity<?> getById(@PathVariable Integer id) {
		try {
			logger.info("Looking for JobSeek.");
			JobSeekEntity wantedJobSeek = jobSeekRepository.findById(id).orElse(null);
			if (wantedJobSeek != null) {
				logger.info("JobSeek found.");
				return new ResponseEntity<JobSeekEntity>(wantedJobSeek, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}
		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.NOT_FOUND);
		// po drakulicevoj preporuci svugde prepraviti da bi min i test bio bolji user frendly
	}

///////////////////////////////////////GET BY EMPLOYEE /////////////////////////////////////

	@Override
	public ResponseEntity<?> getAllLikeEmployee(@PathVariable Integer id) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}

		try {
			logger.info("Looking for jobs with selected employee");
			List<JobSeekEntity> wantedJobSeeks = jobSeekRepository.getAllByEmployeeId(id);
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<List<JobSeekEntity>>(wantedJobSeeks, HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);

	}

////////////////////////////////////////// GET BY JOBTYPE ////////////////////////////////////////

	@Override
	public ResponseEntity<?> getAllLikeJobType(@PathVariable Integer id) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}

		try {
			logger.info("Looking for jobs with selected jobType");
			List<JobSeekEntity> wantedJobSeeks = jobSeekRepository.getAllByTypeId(id);
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<List<JobSeekEntity>>(wantedJobSeeks, HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);

	}

///////////////////////////////////////GET BY CITY /////////////////////////////////////

	@Override
	public ResponseEntity<?> getAllLikeCity(@PathVariable Integer id) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}

		try {
			logger.info("Looking for jobs with selected city");
			List<JobSeekEntity> wantedJobSeeks = jobSeekRepository.getAllByCityId(id);
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<List<JobSeekEntity>>(wantedJobSeeks, HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);

	}

///////////////////////////////////////GET BY DISTANCE TO JOB /////////////////////////////////////

	@Override
	public ResponseEntity<?> getAllJobSeekWhereDistanceIsAndLessThen(@RequestParam Integer distance) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs with 'distance' less then defined.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getDistanceToJob() <= distance) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<List<JobSeekEntity>>(wantedJobSeeks, HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

	///////////////////// GET BY BEGINNING DATE /////////////////////////

	@Override
	public ResponseEntity<?> getAllWithBeginnigDate(@RequestParam Date beginDate) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'beginnigDate'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getBeginningDate() == beginDate) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<List<JobSeekEntity>>(wantedJobSeeks, HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY END DATE /////////////////////////

	@Override
	public ResponseEntity<?> getAllWithEndDate(@RequestParam Date endDate) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'endDate'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getEndDate() == endDate) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<List<JobSeekEntity>>(wantedJobSeeks, HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY FLEXIBILE DATES /////////////////////////

	@Override
	public ResponseEntity<?> getAllWithFlexibileDates(@RequestParam boolean flexDates) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'flexibileDates'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getFlexibileDates() == flexDates) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<List<JobSeekEntity>>(wantedJobSeeks, HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY PRICE /////////////////////////

	@Override
	public ResponseEntity<?> getAllWherePriceIsAndMore(@RequestParam Double price) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'price' and 'price' more then defined.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getPrice() >= price) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<List<JobSeekEntity>>(wantedJobSeeks, HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY FLEXIBILE DAYS /////////////////////////

	@Override
	public ResponseEntity<?> getAllWithFlexibileDays(@RequestParam boolean flexDays) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'flexibileDays'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getFlexibileDays() == flexDays) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<List<JobSeekEntity>>(wantedJobSeeks, HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY STATUS /////////////////////////

	@Override
	public ResponseEntity<?> getAllWithStatus(@RequestParam Integer status) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'status'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getStatus() == status) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<List<JobSeekEntity>>(wantedJobSeeks, HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY ELAPSE /////////////////////////

	@Override
	public ResponseEntity<?> getAllWithExpired(@RequestParam Boolean expired) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'expired'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getExpired() == expired) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<List<JobSeekEntity>>(wantedJobSeeks, HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY CREATED BY /////////////////////////

	@Override
	public ResponseEntity<?> getAllByCreatedBy(@RequestParam Integer createdBy) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'createdBy'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getCreatedById() == createdBy) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<List<JobSeekEntity>>(wantedJobSeeks, HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY CREATED BY /////////////////////////

	@Override
	public ResponseEntity<?> getAllByUpdatedBy(@RequestParam Integer updatedBy) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'updatedBy'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getUpdatedById() == updatedBy) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<List<JobSeekEntity>>(wantedJobSeeks, HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY VERSION /////////////////////////

	@Override
	public ResponseEntity<?> getAllByVersion(@RequestParam Integer version) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'version'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getVersion() == version) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<List<JobSeekEntity>>(wantedJobSeeks, HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

	////////////////// DELETE ////////////////////////////

	@Override
	public ResponseEntity<?> deleteById(@PathVariable Integer id) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		JobSeekEntity wantedJobSeek = new JobSeekEntity();
		try {
			logger.info("Looking for jobSeek.");
			wantedJobSeek = jobSeekRepository.findById(id).orElse(null);
			if (wantedJobSeek == null) {
				logger.info("jobSeek that you asked for doesn't exist.");
				return new ResponseEntity<String>("jobSeek that you asked for doesn't exist.", HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			logger.info("Changing activity.");
			if (wantedJobSeek.getStatus().equals(1)) {
				logger.info("Deleting entity.");
				wantedJobSeek.setStatusInactive();
				// applyContactDaoImp.markApplyAsExpiredBySeek(wantedJobSeek); //DODAO ZA NIDZU
				logger.info("Entity deleted.");
			} else if (wantedJobSeek.getStatus().equals(0)) {
				logger.info("JobSeek status is already deleted.");
				return new ResponseEntity<String>("JobSeek status is already deleted.", HttpStatus.OK);
			} else if (wantedJobSeek.getStatus().equals(-1)) {
				logger.info("JobSeek is arhived it can't be deleted or undeleted.");
				return new ResponseEntity<String>("JobSeek is arhived it can't be deleted or undeleted.",
						HttpStatus.OK);
			} else {
				logger.info("JobSeek has unknown status, check status in datebase for jobSeek.");
				return new ResponseEntity<String>("JobSeek has unknown status, check status in datebase for jobSeek.",
						HttpStatus.OK);
			}
			logger.info("Saveing entity.");
			jobSeekRepository.save(wantedJobSeek);
			logger.info("jobSeek changed.");
		} catch (Exception e) {
			logger.info("Error occurred during 'Deleting jobSeek.'");
			return new ResponseEntity<String>("Error occurred during 'Deleting jobSeek'." + e, HttpStatus.BAD_REQUEST);
		}
		logger.info("Returning jobSeek.");
		return new ResponseEntity<JobSeekEntity>(wantedJobSeek, HttpStatus.OK);
	}

	////////////////////// UNDELETE //////////////////////////////

	@Override
	public ResponseEntity<?> unDeleteById(@PathVariable Integer id) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		JobSeekEntity wantedJobSeek = new JobSeekEntity();
		try {
			logger.info("Looking for jobSeek.");
			wantedJobSeek = jobSeekRepository.findById(id).orElse(null);
			if (wantedJobSeek == null) {
				logger.info("JobSeek that you asked for doesn't exist.");
				return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			logger.info("Changing activity.");
			if (wantedJobSeek.getStatus().equals(0)) {
				logger.info("Undeleteing entity.");
				wantedJobSeek.setStatusActive();
				logger.info("Entity deleted.");
			} else if (wantedJobSeek.getStatus().equals(1)) {
				logger.info("JobSeek status is already activ.");
				return new ResponseEntity<String>("JobSeek status is already activ.", HttpStatus.OK);
			} else if (wantedJobSeek.getStatus().equals(-1)) {
				logger.info("JobSeek is arhived it can't be deleted or undeleted.");
				return new ResponseEntity<String>("JobSeek is arhived it can't be deleted or undeleted.",
						HttpStatus.OK);
			} else {
				logger.info("JobSeek has unknown status, check status in datebase for jobSeek.");
				return new ResponseEntity<String>("JobSeek has unknown status, check status in datebase for jobSeek.",
						HttpStatus.OK);
			}
			logger.info("Saveing entity.");
			jobSeekRepository.save(wantedJobSeek);
			logger.info("jobSeek changed.");
		} catch (Exception e) {
			logger.info("Error occurred during 'Undeleting jobSeek.'");
			return new ResponseEntity<String>("Error occurred during 'Undeleting jobSeek'." + e,
					HttpStatus.BAD_REQUEST);
		}
		logger.info("Returning jobSeek.");
		return new ResponseEntity<JobSeekEntity>(wantedJobSeek, HttpStatus.OK);
	}

	////////////////////// ARCHIVE //////////////////////////

	@Override
	public ResponseEntity<?> archiveById(@PathVariable Integer id) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		JobSeekEntity wantedJobSeek = new JobSeekEntity();
		try {
			logger.info("Looking for jobSeek.");
			wantedJobSeek = jobSeekRepository.findById(id).orElse(null);
			if (wantedJobSeek == null) {
				logger.info("jobSeek that you asked for doesn't exist.");
				return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			logger.info("Changing activity.");
			if (wantedJobSeek.getStatus().equals(0) || wantedJobSeek.getStatus().equals(1)) {
				logger.info("Archiving entity.");
				wantedJobSeek.setStatusArchived();
				// applyContactDaoImp.markApplyAsExpiredBySeek(wantedJobSeek); // DODAO ZA NIDZU
				logger.info("Entity archived.");
			} else if (wantedJobSeek.getStatus().equals(-1)) {
				logger.info("JobSeek status is already archived.");
				return new ResponseEntity<String>("JobSeek status is already archived.", HttpStatus.OK);
			} else {
				logger.info("JobSeek has unknown status, check status in datebase for jobSeek.");
				return new ResponseEntity<String>("JobSeek has unknown status, check status in datebase for jobSeek.",
						HttpStatus.OK);
			}
			logger.info("Saveing entity.");
			jobSeekRepository.save(wantedJobSeek);
			logger.info("jobSeek changed.");
		} catch (Exception e) {
			logger.info("Error occurred during 'Archiveing jobSeek.'");
			return new ResponseEntity<String>("Error occurred during 'Archiveing jobSeek'." + e,
					HttpStatus.BAD_REQUEST);
		}
		logger.info("Returning jobSeek.");
		return new ResponseEntity<JobSeekEntity>(wantedJobSeek, HttpStatus.OK);
	}

	///////////////////// UNARCHIVE ///////////////////////////////

	@Override
	public ResponseEntity<?> unArchiveById(@PathVariable Integer id) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		JobSeekEntity wantedJobSeek = new JobSeekEntity();
		try {
			logger.info("Looking for jobSeek.");
			wantedJobSeek = jobSeekRepository.findById(id).orElse(null);
			if (wantedJobSeek == null) {
				logger.info("JobSeek that you asked for doesn't exist.");
				return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occurred during 'Checking database'.");
			return new ResponseEntity<String>("Error occurred during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			logger.info("Changing activity.");
			if (wantedJobSeek.getStatus().equals(-1)) {
				logger.info("Unarchiving entity.");
				wantedJobSeek.setStatusActive();
				logger.info("Entity unarchived.");
			} else if (wantedJobSeek.getStatus().equals(1) || wantedJobSeek.getStatus().equals(0)) {
				logger.info("JobSeek status is already active.");
				return new ResponseEntity<String>("JobSeek status is already active.", HttpStatus.OK);
			} else {
				logger.info("JobSeek has unknown status, check status in datebase for jobSeek.");
				return new ResponseEntity<String>("JobSeek has unknown status, check status in datebase for jobSeek.",
						HttpStatus.OK);
			}
			logger.info("Saveing entity.");
			jobSeekRepository.save(wantedJobSeek);
			logger.info("jobSeek changed.");
		} catch (Exception e) {
			logger.info("Error occurred during 'Unarchiveing jobSeek.'");
			return new ResponseEntity<String>("Error occurred during 'Unarchiveing jobSeek'." + e,
					HttpStatus.BAD_REQUEST);
		}
		logger.info("Returning jobSeek.");
		return new ResponseEntity<JobSeekEntity>(wantedJobSeek, HttpStatus.OK);
	}

	private String createErrorMessage(BindingResult result) {
		return result.getAllErrors().stream().map(ObjectError::getDefaultMessage).collect(Collectors.joining(" "));
	}

//pagination:

	@Override
	public Page<JobSeekEntity> getAll(Pageable pageable) {
		List<JobSeekEntity> resultList = (List<JobSeekEntity>) jobSeekRepository.findAll();
		Page<JobSeekEntity> resultPage = new PageImpl<>(resultList,
				PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
				resultList.size());
		logger.info("Returning result.");
		return resultPage;
	}

	/////////////////////////////////////// GET BY EMPLOYEE Pageable

	@Override
	public ResponseEntity<?> getAllLikeEmployee(@PathVariable Integer id, Pageable pageable) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}

		try {
			logger.info("Looking for jobs with selected employee");
			List<JobSeekEntity> wantedJobSeeks = jobSeekRepository.getAllByEmployeeId(id);
			if (!wantedJobSeeks.isEmpty()) {
				Page<JobSeekEntity> wantedJobSeeksPage = new PageImpl<>(wantedJobSeeks,
						PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
						wantedJobSeeks.size());

				logger.info("Returning JobSeeks.");
				return new ResponseEntity<Page<JobSeekEntity>>(wantedJobSeeksPage, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);

	}

//////////////////////////////////////////GET BY JOBTYPE ////////////////////////////////////////

	@Override
	public ResponseEntity<?> getAllLikeJobType(@PathVariable Integer id, Pageable pageable) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}

		try {
			logger.info("Looking for jobs with selected jobType");
			List<JobSeekEntity> wantedJobSeeks = jobSeekRepository.getAllByTypeId(id);

			if (!wantedJobSeeks.isEmpty()) {
				Page<JobSeekEntity> wantedJobSeeksPage = new PageImpl<>(wantedJobSeeks,
						PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
						wantedJobSeeks.size());
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<Page<JobSeekEntity>>(wantedJobSeeksPage, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);

	}

///////////////////////////////////////GET BY CITY /////////////////////////////////////

	@Override
	public ResponseEntity<?> getAllLikeCity(@PathVariable Integer id, Pageable pageable) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}

		try {
			logger.info("Looking for jobs with selected city");
			List<JobSeekEntity> wantedJobSeeks = jobSeekRepository.getAllByCityId(id);

			if (!wantedJobSeeks.isEmpty()) {
				Page<JobSeekEntity> wantedJobSeeksPage = new PageImpl<>(wantedJobSeeks,
						PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
						wantedJobSeeks.size());
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<Page<JobSeekEntity>>(wantedJobSeeksPage, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);

	}

///////////////////////////////////////GET BY DISTANCE TO JOB /////////////////////////////////////

	@Override
	public ResponseEntity<?> getAllJobSeekWhereDistanceIsAndLessThen(@RequestParam Integer distance,
			Pageable pageable) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs with 'distance' less then defined.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getDistanceToJob() <= distance) {
					wantedJobSeeks.add(i);
				}
			}

			if (!wantedJobSeeks.isEmpty()) {
				Page<JobSeekEntity> wantedJobSeeksPage = new PageImpl<>(wantedJobSeeks,
						PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
						wantedJobSeeks.size());
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<Page<JobSeekEntity>>(wantedJobSeeksPage, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

	///////////////////// GET BY BEGINNING DATE /////////////////////////

	@Override
	public ResponseEntity<?> getAllWithBeginnigDate(@RequestParam Date beginDate, Pageable pageable) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'beginnigDate'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getBeginningDate() == beginDate) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				Page<JobSeekEntity> wantedJobSeeksPage = new PageImpl<>(wantedJobSeeks,
						PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
						wantedJobSeeks.size());
				return new ResponseEntity<Page<JobSeekEntity>>(wantedJobSeeksPage, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY END DATE /////////////////////////

	@Override
	public ResponseEntity<?> getAllWithEndDate(@RequestParam Date endDate, Pageable pageable) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'endDate'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getEndDate() == endDate) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				Page<JobSeekEntity> wantedJobSeeksPage = new PageImpl<>(wantedJobSeeks,
						PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
						wantedJobSeeks.size());
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<Page<JobSeekEntity>>(wantedJobSeeksPage, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY FLEXIBILE DATES /////////////////////////

	@Override
	public ResponseEntity<?> getAllWithFlexibileDates(@RequestParam boolean flexDates, Pageable pageable) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'flexibileDates'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getFlexibileDates() == flexDates) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				Page<JobSeekEntity> wantedJobSeeksPage = new PageImpl<>(wantedJobSeeks,
						PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
						wantedJobSeeks.size());
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<Page<JobSeekEntity>>(wantedJobSeeksPage, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY FLEXIBILE DAYS /////////////////////////

	@Override
	public ResponseEntity<?> getAllWithFlexibileDays(@RequestParam boolean flexDays, Pageable pageable) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'flexibileDays'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getFlexibileDays() == flexDays) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				Page<JobSeekEntity> wantedJobSeeksPage = new PageImpl<>(wantedJobSeeks,
						PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
						wantedJobSeeks.size());
				return new ResponseEntity<Page<JobSeekEntity>>(wantedJobSeeksPage, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY PRICE /////////////////////////

	@Override
	public ResponseEntity<?> getAllWherePriceIsAndMore(@RequestParam Double price, Pageable pageable) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'price' and 'price' more then defined.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getPrice() >= price) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				Page<JobSeekEntity> wantedJobSeeksPage = new PageImpl<>(wantedJobSeeks,
						PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
						wantedJobSeeks.size());
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<Page<JobSeekEntity>>(wantedJobSeeksPage, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY STATUS /////////////////////////

	@Override
	public ResponseEntity<?> getAllWithStatus(@RequestParam Integer status, Pageable pageable) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'status'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getStatus() == status) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				Page<JobSeekEntity> wantedJobSeeksPage = new PageImpl<>(wantedJobSeeks,
						PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
						wantedJobSeeks.size());
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<Page<JobSeekEntity>>(wantedJobSeeksPage, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY ELAPSE /////////////////////////

	@Override
	public ResponseEntity<?> getAllWithExpired(@RequestParam Boolean expired, Pageable pageable) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'expired'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getExpired() == expired) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				Page<JobSeekEntity> wantedJobSeeksPage = new PageImpl<>(wantedJobSeeks,
						PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
						wantedJobSeeks.size());
				logger.info("Returning JobSeeks.");
				return new ResponseEntity<Page<JobSeekEntity>>(wantedJobSeeksPage, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY CREATED BY /////////////////////////

	@Override
	public ResponseEntity<?> getAllByCreatedBy(@RequestParam Integer createdBy, Pageable pageable) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'createdBy'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getCreatedById() == createdBy) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				// Page<JobSeekEntity> wantedJobSeeksPage = new PageImpl<>(wantedJobSeeks, new
				// PageRequest(pageable.getPageNumber(), pageable.getPageSize(),
				// pageable.getSort()), wantedJobSeeks.size() );
				Page<JobSeekEntity> wantedJobSeeksPage = new PageImpl<>(wantedJobSeeks,
						PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
						wantedJobSeeks.size());

				return new ResponseEntity<Page<JobSeekEntity>>(wantedJobSeeksPage, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

///////////////////// GET BY UPDATED BY /////////////////////////

	@Override
	public ResponseEntity<?> getAllByUpdatedBy(@RequestParam Integer updatedBy, Pageable pageable) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'updatedBy'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getUpdatedById() == updatedBy) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				Page<JobSeekEntity> wantedJobSeeksPage = new PageImpl<>(wantedJobSeeks,
						PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
						wantedJobSeeks.size());

				return new ResponseEntity<Page<JobSeekEntity>>(wantedJobSeeksPage, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

	///////////////////// GET BY VERSION /////////////////////////

	@Override
	public ResponseEntity<?> getAllByVersion(@RequestParam Integer version, Pageable pageable) {
		try {
			logger.info("Checking database.");
			if (((jobSeekRepository.count() == 0))) {
				logger.info("Database empty.");
				return new ResponseEntity<String>("Database empty.", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'." + e, HttpStatus.BAD_REQUEST);
		}
		try {
			List<JobSeekEntity> wantedJobSeeks = new ArrayList<JobSeekEntity>();
			logger.info("Looking for jobs defined with 'version'.");
			for (JobSeekEntity i : jobSeekRepository.findAll()) {
				if (i.getVersion() == version) {
					wantedJobSeeks.add(i);
				}
			}
			if (!wantedJobSeeks.isEmpty()) {
				logger.info("Returning JobSeeks.");
				Page<JobSeekEntity> wantedJobSeeksPage = new PageImpl<>(wantedJobSeeks,
						PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort()),
						wantedJobSeeks.size());

				return new ResponseEntity<Page<JobSeekEntity>>(wantedJobSeeksPage, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.info("Error occured during 'Checking database'.");
			return new ResponseEntity<String>("Error occured during 'Checking database'.", HttpStatus.BAD_REQUEST);
		}

		logger.info("JobSeek that you asked for doesn't exist.");
		return new ResponseEntity<String>("JobSeek that you asked for doesn't exist.", HttpStatus.BAD_REQUEST);
	}

	// MOCNI SEARCH

	@SuppressWarnings("unchecked")
	@Override
	public ResponseEntity<?> findByQuery(/*Boolean flexibileHours, Integer fromHour, Integer toHour, 
			Boolean IsMinMax, */UserEntity loggedUser, List<JobDayHoursDTO> jobDayHours, Integer employeeId, String cityName, String countryRegionName, String countryName, 
			Integer typeId, Date beginningDate, Date endDate, Boolean flexibileDates, Double price, 
			Boolean flexibileDays) throws Exception {
		logger.info("++++++++++++++++ Service for finding JobSeeks");
//		String sql = "select js from JobSeekEntity js join js.daysAndHours dh join js.type t join js.city c join js.employee e join c.toDistances td join c.fromDistances fd where js.status =1 and js.expired = false and dh.status = 1 and t.status = 1 and c.status = 1 and e.status = 1 and td.kmDistance <= js.distanceToJob and fd.kmDistance <= js.distanceToJob";
		String sql = "select DISTINCT js from JobSeekEntity js join js.daysAndHours dh join js.type t join js.city c join js.employee e where js.status =1 and js.expired = false and dh.status = 1 and t.status = 1 and c.status = 1 and e.status = 1" +
					" AND js.id  NOT IN (select rs.rejectedSeek.id from RejectSeekEntity rs where rs.user.id = " + loggedUser.getId() + ")" + 
					" AND js.id  NOT IN (select ac.seek.id from ApplyContactEntity ac where ac.offer.employer.id = " + loggedUser.getId() + " AND ac.offer.counterOffer = true AND ac.seek.counterSeek = false)";
		logger.info("++++++++++++++++ Basic query created");

//		if (flexibileHours != null) {
//			sql = sql + " and dh.flexibileHours = " + flexibileHours;
//			logger.info("++++++++++++++++ Added condition for flexibileHours applications");
//		}
//		
//		if (fromHour != null) {
//			sql = sql + " and dh.fromHour = " + fromHour;
//			logger.info("++++++++++++++++ Added condition for fromHour applications");
//		}
//		
//		if (toHour != null) {
//			sql = sql + " and dh.toHour = " + toHour;
//			logger.info("++++++++++++++++ Added condition for toHour applications");
//		}
//
//		if (IsMinMax != null) {
//			sql = sql + " and dh.isMinMax = " + IsMinMax;
//			logger.info("++++++++++++++++ Added condition for IsMinMax applications");
//		}

//		if (connected != null) {
//			sql = sql + " and a.areConnected = " + connected;
//			if (connected) {
//				logger.info("++++++++++++++++ Added condition for connected applications");
//			} else {
//				logger.info("++++++++++++++++ Added condition for pending applications");
//			}
//		}
				
		if (employeeId != null) {
			sql = sql + " and js.employee.id = " + employeeId;
			logger.info("++++++++++++++++ Added condition for employee applications");
		}
		
		if (cityName != null && countryName != null) {
			CityEntity city = new CityEntity();
			CountryEntity country = new CountryEntity();
			CountryRegionEntity countryRegion = new CountryRegionEntity();
			try {
				country = countryRepository.findByCountryNameIgnoreCase(countryName);
			} catch (Exception e) {
				throw new Exception("CountryRepository failed.");
			}
			if (country == null) {
				logger.info("Country doesn't exist in database.");
				return new ResponseEntity<String>("Country doesn't exist in database.", HttpStatus.NOT_FOUND);
			}
			try {
				countryRegion = countryRegionRepository.findByCountryRegionNameAndCountry(countryRegionName, country);
			} catch (Exception e) {
				throw new Exception("CountryRegionRepository failed.");
			}
			if (countryRegion == null) {
				logger.info("Country region doesn't exist in database.");
				return new ResponseEntity<String>("Country region doesn't exist in database.", HttpStatus.NOT_FOUND);
			}
			try {
				city = cityRepository.findByCityNameAndRegion(cityName, countryRegion);
			} catch (Exception e) {
				throw new Exception("CityRepository failed.");
			}
			if (city == null) {
				logger.info("City doesn't exist in database.");
				return new ResponseEntity<String>("City doesn't exist in database.", HttpStatus.NOT_FOUND);
			}
			logger.info("City founded.");
			sql = sql + " and (js.city.id = " + city.getId() + " or " + city.getId() + " IN(select cd.fromCity.id from CityDistanceEntity cd where cd.kmDistance <= js.distanceToJob and cd.toCity.id = js.city.id) or " + city.getId() + " IN(select cd.toCity.id from CityDistanceEntity cd where cd.kmDistance <= js.distanceToJob and cd.fromCity.id = js.city.id))";
			logger.info("++++++++++++++++ Added condition for city applications");
		}
		
//		if (city != null) {
//			sql = sql + " and js.city = " + city;
//			logger.info("++++++++++++++++ Added condition for city applications");
//		}
		
		if (typeId != null) {
			sql = sql + " and js.type.id = " + typeId;
			logger.info("++++++++++++++++ Added condition for type applications");
		}
		
//		if (beginningDate != null) {
//			sql = sql + " and js.beginningDate = " + beginningDate;
//			logger.info("++++++++++++++++ Added condition for beginningDate applications");
//		}
//		
//		if (endDate != null) {
//			sql = sql + " and js.endDate = " + endDate;
//			logger.info("++++++++++++++++ Added condition for endDate applications");
//		}
//		
//		if (flexibileDates != null) {
//			sql = sql + " and js.flexibileDates = " + flexibileDates;
//			logger.info("++++++++++++++++ Added condition for flexibileDates applications");
//		}
		
		if (flexibileDates != null && flexibileDates == true) {
			if (beginningDate != null && endDate != null) {
				sql = sql + " and ((js.flexibileDates = true" +
						" and (('" + beginningDate + "' BETWEEN js.beginningDate and js.endDate and '" + endDate + "' BETWEEN js.beginningDate and js.endDate) or " +
						"('" + beginningDate + "' < js.beginningDate and '" + endDate + "' BETWEEN js.beginningDate and js.endDate) or " +
						"('" + endDate + "' > js.endDate and '" + beginningDate + "' BETWEEN js.beginningDate and js.endDate) or " +
						"(js.beginningDate BETWEEN '" + beginningDate + "' and '" + endDate + "' and js.endDate BETWEEN '" + beginningDate + "' and '" + endDate + "'))) or " +
						"(js.flexibileDates = false" +
						" and js.beginningDate BETWEEN '" + beginningDate + "' and '" + endDate + "' and js.endDate BETWEEN '" + beginningDate + "' and '" + endDate + "'))";
				logger.info("++++++++++++++++ Added condition for beginningDate and endDate applications");
			} else if (beginningDate != null) {
				sql = sql + " and ((js.flexibileDates = true" +
						" and '" + beginningDate + "' < js.endDate) or " +
						"(js.flexibileDates = false" +
						" and js.beginningDate >= '" + beginningDate + "'))";
				logger.info("++++++++++++++++ Added condition for beginningDate applications");
			} else if (endDate != null) {
				sql = sql + " and ((js.flexibileDates = true" +
						" and '" + endDate + "' > js.beginningDate) or " +
						"(js.flexibileDates = false" +
						" and js.endDate <= '" + endDate + "'))";
				logger.info("++++++++++++++++ Added condition for endDate applications");
			} /*else {
				sql = sql + " and js.flexibileDates = true";
			}*/
			logger.info("++++++++++++++++ Added condition for TRUE flexibileDates applications");
		} else {
			if (beginningDate != null && endDate != null) {
				sql = sql + " and ((js.flexibileDates = true" +
						" and '" + beginningDate + "' BETWEEN js.beginningDate and js.endDate and '" + endDate + "' BETWEEN js.beginningDate and js.endDate) or " +
						"(js.flexibileDates = false" +
						" and js.beginningDate = '" + beginningDate + "' and js.endDate = '" + endDate + "'))";
				logger.info("++++++++++++++++ Added condition for beginningDate and endDate applications");
			} else if (beginningDate != null) {
				sql = sql + " and ((js.flexibileDates = true" +
						" and '" + beginningDate + "' BETWEEN js.beginningDate and js.endDate) or " +
						"(js.flexibileDates = false" +
						" and js.beginningDate = '" + beginningDate + "'))";
				logger.info("++++++++++++++++ Added condition for beginningDate applications");
			} else if (endDate != null) {
				sql = sql + " and ((js.flexibileDates = true" +
						" and '" + endDate + "' BETWEEN js.beginningDate and js.endDate) or " +
						"(js.flexibileDates = false" +
						" and js.endDate = '" + endDate + "'))";
				logger.info("++++++++++++++++ Added condition for endDate applications");
			} /*else {
				sql = sql + " and js.flexibileDates = false";
			}*/
			logger.info("++++++++++++++++ Added condition for FALSE flexibileDates applications");
		}
		
		if (price != null) {
			sql = sql + " and js.price <= " + price;
			logger.info("++++++++++++++++ Added condition for price applications");
		}
		
//		if (connectionDateBottom != null) {
//			sql = sql + " and a.connectionDate >= '" + connectionDateBottom + "'";
//			logger.info("++++++++++++++++ Added condition for applications where connection is younger than"
//					+ connectionDateBottom);
//		}
//
//		if (connectionDateTop != null) {
//			sql = sql + " and a.connectionDate <= '" + connectionDateTop + "'";
//			logger.info("++++++++++++++++ Added condition for applications where conncetion is older than"
//					+ connectionDateTop);
//		}

//		if (contactDateTop != null) {
//			sql = sql + " and a.contactDate <= '" + contactDateTop + "'";
//			logger.info("++++++++++++++++ Added condition for all applications where contact is older than "
//					+ contactDateTop);
//		}
//
//		if (contactDateBottom != null) {
//			sql = sql + " and a.contactDate >= '" + contactDateBottom + "'";
//			logger.info("++++++++++++++++ Added condition for all applications where contact is younger than "
//					+ contactDateBottom);
//		}

		
		if (flexibileDays != null && flexibileDays == true) {
			Integer counter = 0;
			if (jobDayHours != null && !jobDayHours.isEmpty()) {
				
//				sql = sql + " and (js.flexibileDays = true" +
//						" and (dh.day IN (" + jobDayHours.stream()
//														.map(JobDayHoursDTO::getDay)
//														.collect(Collectors.toList()) + ") and ";
				
				sql = sql + " AND ((js.flexibileDays = true and (";
				counter = jobDayHours.size();
				for (JobDayHoursDTO jdh : jobDayHours) {
					sql = sql + "(dh.day = '" + jdh.getDay() + "' and ((dh.isMinMax = " + jdh.getIsMinMax() + " and ";
					if (jdh.getFlexibileHours() != null && jdh.getFlexibileHours() == true) {
						sql = sql + "((dh.flexibileHours = true AND ((" +
								jdh.getFromHour() + " <= dh.fromHour AND " +
								jdh.getFromHour() + " < dh.toHour AND " +
								jdh.getToHour() + " > dh.fromHour AND " +
								jdh.getToHour() + " >= dh.toHour) OR (" +
								jdh.getFromHour() + " >= dh.fromHour AND " +
								jdh.getFromHour() + " < dh.toHour AND " +
								jdh.getToHour() + " <= dh.toHour AND " +
								jdh.getToHour() + " > dh.fromHour) OR (" +
								jdh.getFromHour() + " < dh.fromHour AND " +
								jdh.getToHour() + " <= dh.toHour AND " +
								jdh.getToHour() + " >= dh.fromHour) OR (" +
								jdh.getFromHour() + " >= dh.fromHour AND " +
								jdh.getFromHour() + " <= dh.toHour AND " +
								jdh.getToHour() + " > dh.toHour))) OR " +
								"(dh.flexibileHours = false AND " +
								jdh.getFromHour() + " <= dh.fromHour AND " +
								jdh.getFromHour() + " < dh.toHour AND " +
								jdh.getToHour() + " > dh.fromHour AND " +
								jdh.getToHour() + " >= dh.toHour)))";
					} else {
						sql = sql + "((dh.flexibileHours = true AND " +
								jdh.getFromHour() + " >= dh.fromHour AND " +
								jdh.getFromHour() + " < dh.toHour AND " +
								jdh.getToHour() + " > dh.fromHour AND " +
								jdh.getToHour() + " <= dh.toHour) OR " +
								"(dh.flexibileHours = false AND " +
								jdh.getFromHour() + " = dh.fromHour AND " +
								jdh.getToHour() + " = dh.toHour)))";
					}
					if (jdh.getIsMinMax() != null && jdh.getIsMinMax() == true && jdh.getFromHour() != null && jdh.getToHour() != null) {
						sql = sql + " OR (dh.isMinMax <> " + jdh.getIsMinMax() + " and dh.isMinMax = false";
						if (jdh.getFlexibileHours() != null && jdh.getFlexibileHours() == false) {
							sql = sql + " and " +
								jdh.getFromHour() + " <= (dh.toHour - dh.fromHour) AND " +
								jdh.getToHour() + " >= (dh.toHour - dh.fromHour)";
						}
						sql = sql + ")";
					} else if (jdh.getIsMinMax() != null && jdh.getIsMinMax() == false && jdh.getFromHour() != null && jdh.getToHour() != null) {
						sql = sql + " OR (dh.isMinMax <> " + jdh.getIsMinMax() + " and dh.isMinMax = true" +
								" and ((dh.flexibileHours = true) or (dh.flexibileHours = false and " +
								"dh.fromHour <= " + (jdh.getToHour() - jdh.getFromHour()) + " AND " +
								"dh.toHour >= " + (jdh.getToHour() - jdh.getFromHour()) + ")))";
					}	
					counter--;
					if (counter >= 1) {
						sql = sql + ")) OR ";
					} else {
						sql = sql + "))))";
					}
				}
				logger.info("++++++++++++++++ Added condition for TRUE flexibileDays, jobDayHours and TRUE js.flexibileDays applications");

//				logger.info(sql);
				
				sql = sql + " OR (js.flexibileDays = false and (";
				counter = jobDayHours.size();
				for (JobDayHoursDTO jdh : jobDayHours) {
					sql = sql + "(dh.day = '" + jdh.getDay() + "' and ((dh.isMinMax = " + jdh.getIsMinMax() + " and ";
					if (jdh.getFlexibileHours() != null && jdh.getFlexibileHours() == true) {
						sql = sql + "((dh.flexibileHours = true AND ((" +
								jdh.getFromHour() + " <= dh.fromHour AND " +
								jdh.getFromHour() + " < dh.toHour AND " +
								jdh.getToHour() + " > dh.fromHour AND " +
								jdh.getToHour() + " >= dh.toHour) OR (" +
								jdh.getFromHour() + " >= dh.fromHour AND " +
								jdh.getFromHour() + " < dh.toHour AND " +
								jdh.getToHour() + " <= dh.toHour AND " +
								jdh.getToHour() + " > dh.fromHour) OR (" +
								jdh.getFromHour() + " < dh.fromHour AND " +
								jdh.getToHour() + " <= dh.toHour AND " +
								jdh.getToHour() + " >= dh.fromHour) OR (" +
								jdh.getFromHour() + " >= dh.fromHour AND " +
								jdh.getFromHour() + " <= dh.toHour AND " +
								jdh.getToHour() + " > dh.toHour))) OR " +
								"(dh.flexibileHours = false AND " +
								jdh.getFromHour() + " <= dh.fromHour AND " +
								jdh.getFromHour() + " < dh.toHour AND " +
								jdh.getToHour() + " > dh.fromHour AND " +
								jdh.getToHour() + " >= dh.toHour)))";
					} else {
						sql = sql + "((dh.flexibileHours = true AND " +
								jdh.getFromHour() + " >= dh.fromHour AND " +
								jdh.getFromHour() + " < dh.toHour AND " +
								jdh.getToHour() + " > dh.fromHour AND " +
								jdh.getToHour() + " <= dh.toHour) OR " +
								"(dh.flexibileHours = false AND " +
								jdh.getFromHour() + " = dh.fromHour AND " +
								jdh.getToHour() + " = dh.toHour)))";
					}
					if (jdh.getIsMinMax() != null && jdh.getIsMinMax() == true && jdh.getFromHour() != null && jdh.getToHour() != null) {
						sql = sql + " OR (dh.isMinMax <> " + jdh.getIsMinMax() + " and dh.isMinMax = false";
						if (jdh.getFlexibileHours() != null && jdh.getFlexibileHours() == false) {
							sql = sql + " and " +
								jdh.getFromHour() + " <= (dh.toHour - dh.fromHour) AND " +
								jdh.getToHour() + " >= (dh.toHour - dh.fromHour)";
						}
						sql = sql + ")";
					} else if (jdh.getIsMinMax() != null && jdh.getIsMinMax() == false && jdh.getFromHour() != null && jdh.getToHour() != null) {
						sql = sql + " OR (dh.isMinMax <> " + jdh.getIsMinMax() + " and dh.isMinMax = true" +
								" and ((dh.flexibileHours = true) or (dh.flexibileHours = false and " +
								"dh.fromHour <= " + (jdh.getToHour() - jdh.getFromHour()) + " AND " +
								"dh.toHour >= " + (jdh.getToHour() - jdh.getFromHour()) + ")))";
					}	
					counter--;
					if (counter >= 1) {
						sql = sql + ")) OR ";
					} else {
						sql = sql + ")))))";
					}
				}
				logger.info("++++++++++++++++ Added condition for TRUE flexibileDays, jobDayHours and FALSE js.flexibileDays applications");
				
//				logger.info(sql);
			}

			sql = sql + " GROUP BY js.id HAVING (COUNT(*) = (select COUNT(*) from JobDayHoursEntity jdh where jdh.status = 1 and jdh.seek.id = js.id) AND js.flexibileDays = false) OR (COUNT(*) > 0 AND js.flexibileDays = true)";

			logger.info("++++++++++++++++ Added condition for TRUE flexibileDays applications");
				
		} else {
			Integer counter = 0;
			if (jobDayHours != null && !jobDayHours.isEmpty()) {
				sql = sql + " AND ((js.flexibileDays = true and (";
				counter = jobDayHours.size();
				for (JobDayHoursDTO jdh : jobDayHours) {
					sql = sql + "(dh.day = '" + jdh.getDay() + "' and ((dh.isMinMax = " + jdh.getIsMinMax() + " and ";
					if (jdh.getFlexibileHours() != null && jdh.getFlexibileHours() == true) {
						sql = sql + "((dh.flexibileHours = true AND ((" +
								jdh.getFromHour() + " <= dh.fromHour AND " +
								jdh.getFromHour() + " < dh.toHour AND " +
								jdh.getToHour() + " > dh.fromHour AND " +
								jdh.getToHour() + " >= dh.toHour) OR (" + 
								jdh.getFromHour() + " >= dh.fromHour AND " +
								jdh.getFromHour() + " < dh.toHour AND " +
								jdh.getToHour() + " <= dh.toHour AND " +
								jdh.getToHour() + " > dh.fromHour) OR (" + 
								jdh.getFromHour() + " < dh.fromHour AND " +
								jdh.getToHour() + " <= dh.toHour AND " +
								jdh.getToHour() + " >= dh.fromHour) OR (" + 
								jdh.getFromHour() + " >= dh.fromHour AND " +
								jdh.getFromHour() + " <= dh.toHour AND " +
								jdh.getToHour() + " > dh.toHour))) OR " +
								"(dh.flexibileHours = false AND " +
								jdh.getFromHour() + " <= dh.fromHour AND " +
								jdh.getFromHour() + " < dh.toHour AND " +
								jdh.getToHour() + " > dh.fromHour AND " +
								jdh.getToHour() + " >= dh.toHour)))";
					} else {
						sql = sql + "((dh.flexibileHours = true AND " +
								jdh.getFromHour() + " >= dh.fromHour AND " +
								jdh.getFromHour() + " < dh.toHour AND " +
								jdh.getToHour() + " > dh.fromHour AND " +
								jdh.getToHour() + " <= dh.toHour) OR " +
								"(dh.flexibileHours = false AND " +
								jdh.getFromHour() + " = dh.fromHour AND " +
								jdh.getToHour() + " = dh.toHour)))";
					}
					if (jdh.getIsMinMax() != null && jdh.getIsMinMax() == true && jdh.getFromHour() != null && jdh.getToHour() != null) {
						sql = sql + " OR (dh.isMinMax <> " + jdh.getIsMinMax() + " and dh.isMinMax = false";
						if (jdh.getFlexibileHours() != null && jdh.getFlexibileHours() == false) {
							sql = sql + " and " +
								jdh.getFromHour() + " <= (dh.toHour - dh.fromHour) AND " +
								jdh.getToHour() + " >= (dh.toHour - dh.fromHour)";
						}
						sql = sql + ")";
					} else if (jdh.getIsMinMax() != null && jdh.getIsMinMax() == false && jdh.getFromHour() != null && jdh.getToHour() != null) {
						sql = sql + " OR (dh.isMinMax <> " + jdh.getIsMinMax() + " and dh.isMinMax = true" +
								" and ((dh.flexibileHours = true) or (dh.flexibileHours = false and " +
								"dh.fromHour <= " + (jdh.getToHour() - jdh.getFromHour()) + " AND " +
								"dh.toHour >= " + (jdh.getToHour() - jdh.getFromHour()) + ")))";
					}	
					counter--;
					if (counter >= 1) {
						sql = sql + ")) OR ";
					} else {
						sql = sql + "))))";
					}
				}
				logger.info("++++++++++++++++ Added condition for FALSE flexibileDays, jobDayHours and TRUE js.flexibileDays applications");

//				logger.info(sql);
				
				sql = sql + " OR (js.flexibileDays = false and " + jobDayHours.size() + " = (select COUNT(*) from JobDayHoursEntity jdh where jdh.status = 1 and jdh.seek.id = js.id) and (";
				counter = jobDayHours.size();
				for (JobDayHoursDTO jdh : jobDayHours) {
					sql = sql + "(dh.day = '" + jdh.getDay() + "' and ((dh.isMinMax = " + jdh.getIsMinMax() + " and ";
					if (jdh.getFlexibileHours() != null && jdh.getFlexibileHours() == true) {
						sql = sql + "((dh.flexibileHours = true AND ((" +
								jdh.getFromHour() + " <= dh.fromHour AND " +
								jdh.getFromHour() + " < dh.toHour AND " +
								jdh.getToHour() + " > dh.fromHour AND " +
								jdh.getToHour() + " >= dh.toHour) OR (" +
								jdh.getFromHour() + " >= dh.fromHour AND " +
								jdh.getFromHour() + " < dh.toHour AND " +
								jdh.getToHour() + " <= dh.toHour AND " +
								jdh.getToHour() + " > dh.fromHour) OR (" +
								jdh.getFromHour() + " < dh.fromHour AND " +
								jdh.getToHour() + " <= dh.toHour AND " +
								jdh.getToHour() + " >= dh.fromHour) OR (" +
								jdh.getFromHour() + " >= dh.fromHour AND " +
								jdh.getFromHour() + " <= dh.toHour AND " +
								jdh.getToHour() + " > dh.toHour))) OR " +
								"(dh.flexibileHours = false AND " +
								jdh.getFromHour() + " <= dh.fromHour AND " +
								jdh.getFromHour() + " < dh.toHour AND " +
								jdh.getToHour() + " > dh.fromHour AND " +
								jdh.getToHour() + " >= dh.toHour)))";
					} else {
						sql = sql + "((dh.flexibileHours = true AND " +
								jdh.getFromHour() + " >= dh.fromHour AND " +
								jdh.getFromHour() + " < dh.toHour AND " +
								jdh.getToHour() + " > dh.fromHour AND " +
								jdh.getToHour() + " <= dh.toHour) OR " +
								"(dh.flexibileHours = false AND " +
								jdh.getFromHour() + " = dh.fromHour AND " +
								jdh.getToHour() + " = dh.toHour)))";
					}
					if (jdh.getIsMinMax() != null && jdh.getIsMinMax() == true && jdh.getFromHour() != null && jdh.getToHour() != null) {
						sql = sql + " OR (dh.isMinMax <> " + jdh.getIsMinMax() + " and dh.isMinMax = false";
						if (jdh.getFlexibileHours() != null && jdh.getFlexibileHours() == false) {
							sql = sql + " and " +
								jdh.getFromHour() + " <= (dh.toHour - dh.fromHour) AND " +
								jdh.getToHour() + " >= (dh.toHour - dh.fromHour)";
						}
						sql = sql + ")";
					} else if (jdh.getIsMinMax() != null && jdh.getIsMinMax() == false && jdh.getFromHour() != null && jdh.getToHour() != null) {
						sql = sql + " OR (dh.isMinMax <> " + jdh.getIsMinMax() + " and dh.isMinMax = true" +
								" and ((dh.flexibileHours = true) or (dh.flexibileHours = false and " +
								"dh.fromHour <= " + (jdh.getToHour() - jdh.getFromHour()) + " AND " +
								"dh.toHour >= " + (jdh.getToHour() - jdh.getFromHour()) + ")))";
					}	
					counter--;
					if (counter >= 1) {
						sql = sql + ")) OR ";
					} else {
						sql = sql + ")))))";
					}
				}
				logger.info("++++++++++++++++ Added condition for FALSE flexibileDays, jobDayHours and FALSE js.flexibileDays applications");
				
				sql = sql + " GROUP BY js.id HAVING (COUNT(*) = (select COUNT(*) from JobDayHoursEntity jdh where jdh.status = 1 and jdh.seek.id = js.id) AND COUNT(*) = " + jobDayHours.size() + " AND js.flexibileDays = false) OR (COUNT(*) = " + jobDayHours.size() + " AND js.flexibileDays = true)";

//				logger.info(sql);
			} else {
				
			sql = sql + " GROUP BY js.id HAVING (COUNT(*) = (select COUNT(*) from JobDayHoursEntity jdh where jdh.status = 1 and jdh.seek.id = js.id) AND js.flexibileDays = false) OR (js.flexibileDays = true)";

			}
			
			logger.info("++++++++++++++++ Added condition for FALSE flexibileDays applications");
			
						
		}
		
		Query query = em.createQuery(sql);
		logger.info("++++++++++++++++ Query created");
//		logger.info(sql);
		Iterable<ApplyContactEntity> result = query.getResultList();
		logger.info("++++++++++++++++ Result of the query returned ok");
		return new ResponseEntity<Iterable<ApplyContactEntity>>(result, HttpStatus.OK);

	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ResponseEntity<?> findCounterSeek(UserEntity loggedUser, List<JobDayHoursDTO> jobDayHours, String cityName, String countryRegionName, String countryName, 
			Integer typeId, Integer distanceToJob, Date beginningDate, Date endDate, Boolean flexibileDates, Double price, 
			Boolean flexibileDays, String detailsLink) throws Exception {
		logger.info("++++++++++++++++ Service for finding Counter JobSeek");
		String sql = "select DISTINCT js from JobSeekEntity js join js.daysAndHours dh join js.type t join js.city c "
				+ "join js.employee e where js.status =1 and js.expired = false and dh.status = 1 and t.status = 1 and"
				+ " c.status = 1 and e.status = 1 and js.counterSeek = true";
		logger.info("++++++++++++++++ Basic query created");

		if (loggedUser != null) {
			sql = sql + " and js.employee.id = " + loggedUser.getId();
			logger.info("++++++++++++++++ Added condition for employee applications");
		}
		
		if (cityName != null && countryName != null) {
			CityEntity city = new CityEntity();
			CountryEntity country = new CountryEntity();
			CountryRegionEntity countryRegion = new CountryRegionEntity();
			try {
				country = countryRepository.findByCountryNameIgnoreCase(countryName);
			} catch (Exception e) {
				throw new Exception("CountryRepository failed.");
			}
			if (country == null) {
				logger.info("Country doesn't exist in database.");
				return new ResponseEntity<String>("Country doesn't exist in database.", HttpStatus.NOT_FOUND);
			}
			try {
				countryRegion = countryRegionRepository.findByCountryRegionNameAndCountry(countryRegionName, country);
			} catch (Exception e) {
				throw new Exception("CountryRegionRepository failed.");
			}
			if (countryRegion == null) {
				logger.info("Country region doesn't exist in database.");
				return new ResponseEntity<String>("Country region doesn't exist in database.", HttpStatus.NOT_FOUND);
			}
			try {
				city = cityRepository.findByCityNameAndRegion(cityName, countryRegion);
			} catch (Exception e) {
				throw new Exception("CityRepository failed.");
			}
			if (city == null) {
				logger.info("City doesn't exist in database.");
				return new ResponseEntity<String>("City doesn't exist in database.", HttpStatus.NOT_FOUND);
			}
			logger.info("City founded.");
			sql = sql + " and js.city.id = " + city.getId();
			logger.info("++++++++++++++++ Added condition for city applications");
		}
			
		if (distanceToJob != null) {
			sql = sql + " and js.distanceToJob = " + distanceToJob;
			logger.info("++++++++++++++++ Added condition for distanceToJob applications");
		}

		if (typeId != null) {
			sql = sql + " and js.type.id = " + typeId;
			logger.info("++++++++++++++++ Added condition for type applications");
		}
		
		if (beginningDate != null) {
			sql = sql + " and js.beginningDate = " + beginningDate;
			logger.info("++++++++++++++++ Added condition for beginningDate applications");
		}
		
		if (endDate != null) {
			sql = sql + " and js.endDate = " + endDate;
			logger.info("++++++++++++++++ Added condition for endDate applications");
		}
		
		if (flexibileDates != null) {
			sql = sql + " and js.flexibileDates = " + flexibileDates;
			logger.info("++++++++++++++++ Added condition for flexibileDates applications");
		}
		
		if (price != null) {
			sql = sql + " and js.price = " + price;
			logger.info("++++++++++++++++ Added condition for price applications");
		}
		
		if (detailsLink != null) {
			sql = sql + " and js.detailsLink = " + detailsLink;
			logger.info("++++++++++++++++ Added condition for detailsLink applications");
		}
		
		if (flexibileDays != null) {
			sql = sql + " and js.flexibileDays = " + flexibileDays;
			logger.info("++++++++++++++++ Added condition for flexibileDays applications");
		}
		
		if (jobDayHours != null && !jobDayHours.isEmpty()) {
			Integer counter = jobDayHours.size();
			sql = sql + " and (";
			for (JobDayHoursDTO jdh : jobDayHours) {
				sql = sql + "(dh.day = '" + jdh.getDay() + "' and dh.isMinMax = " + jdh.getIsMinMax() + 
						" and dh.fromHour = " + jdh.getFromHour() + " and dh.toHour = " + jdh.getToHour()+ 
						" and dh.flexibileHours = " + jdh.getFlexibileHours();
				counter--;
				if (counter >= 1) {
					sql = sql + ") OR ";
				} else {
					sql = sql + "))";
				}
			}
			sql = sql + " GROUP BY js.id HAVING (COUNT(*) = (select COUNT(*) from JobDayHoursEntity jdh where "
					+ "jdh.status = 1 and jdh.seek.id = js.id) AND COUNT(*) = " + jobDayHours.size() + ")";
			logger.info("++++++++++++++++ Added condition for jobDayHours applications");
		}
		
		Query query = em.createQuery(sql);
		logger.info("++++++++++++++++ Query created");
//		logger.info(sql);
		Iterable<ApplyContactEntity> result = query.getResultList();
		logger.info("++++++++++++++++ Result of the query returned ok");
		return new ResponseEntity<Iterable<ApplyContactEntity>>(result, HttpStatus.OK);

	}
	
}
