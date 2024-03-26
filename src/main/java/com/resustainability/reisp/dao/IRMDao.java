package com.resustainability.reisp.dao;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.resustainability.reisp.common.DBConnectionHandler;
import com.resustainability.reisp.common.DateParser;
import com.resustainability.reisp.common.EMailSender;
import com.resustainability.reisp.common.FileUploads;
import com.resustainability.reisp.common.Mail;
import com.resustainability.reisp.common.OSValidator;
import com.resustainability.reisp.common.UrlGenerator;
import com.resustainability.reisp.constants.CommonConstants;
import com.resustainability.reisp.model.IRM;
import com.resustainability.reisp.model.Project;
import com.resustainability.reisp.model.ProjectLocation;
import com.resustainability.reisp.model.RoleMapping;
import com.resustainability.reisp.model.User;
import com.resustainability.reisp.common.CommonMethods;




@Repository
public class IRMDao {

	@Autowired
	JdbcTemplate jdbcTemplate;
	
	@Autowired
	DataSource dataSource;

	@Autowired
	DataSourceTransactionManager transactionManager;

	public List<Project> getProjectstList(User obj) throws SQLException {
		List<Project> menuList = null;
		try{  
			String qry = "select project_code,sbu_code,project_name from [project] where status <> 'Inactive' ";
			menuList = jdbcTemplate.query( qry, new BeanPropertyRowMapper<Project>(Project.class));
			
		}catch(Exception e){ 
			e.printStackTrace();
			throw new SQLException(e.getMessage());
		}
		return menuList;
	}

	public List<RoleMapping> getDepartments(RoleMapping obj) throws Exception {
		List<RoleMapping> objsList = new ArrayList<RoleMapping>();
		try {
			/*
			 * String qry =
			 * "SELECT  d.department_code ,p.department_name,d.project as project_code FROM [role_mapping] d "
			 * + "  left join department p on d.department_code =  p.department_code " +
			 * " where role_code like '%L1' and d.department_code is not null and  d.department_code <> ''  "
			 * ;
			 */ 
			
			  String qry =
			 "SELECT rm.department_code ,department_name,assigned_to_sbu,project_code FROM [department] d "
			  +
			  "  left join project p on d.assigned_to_sbu like CONCAT('%',p.sbu_code, '%') "+
			  "  left join role_mapping rm on d.department_code = rm.department_code "
			  + " where d.department_code is not null and  d.department_code <> '' and rm.department_code <> 'EHS' and rm.department_code <> 'PH'  ";
			 
			int arrSize = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject())) {
				qry = qry + " and project_code = ? ";
				arrSize++;
			}
			qry = qry + " group by rm.department_code,department_name,assigned_to_sbu,project_code,d.department_code order by   d.department_code asc";
			Object[] pValues = new Object[arrSize];
			int i = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject())) {
				pValues[i++] = obj.getProject();
			}
	
			objsList = jdbcTemplate.query( qry, pValues, new BeanPropertyRowMapper<RoleMapping>(RoleMapping.class));
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		return objsList;
	}

	public List<ProjectLocation> getLocations(ProjectLocation obj) throws Exception {
		List<ProjectLocation> objsList = new ArrayList<ProjectLocation>();
		try {
			String qry = "SELECT distinct s.location_code,s.location_name FROM [project_location] s "
					+ " where  s.location_code is not null and  s.location_code <> ''  "; 
			int arrSize = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				qry = qry + " and s.project_code = ? ";
				arrSize++;
			}
		
			qry = qry + " order by   s.location_code asc";
			Object[] pValues = new Object[arrSize];
			int i = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				pValues[i++] = obj.getProject_code();
			}
	
			objsList = jdbcTemplate.query( qry, pValues, new BeanPropertyRowMapper<ProjectLocation>(ProjectLocation.class));
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		return objsList;
	}

	public List<RoleMapping> getRoleMappingforIRMForm(RoleMapping obj) throws Exception {
		List<RoleMapping> objsList = new ArrayList<RoleMapping>();
		try {
			String qry = "SELECT distinct s.employee_code,c.user_id,c.user_name,s.role_code,c.email_id FROM [role_mapping] s "
					+ " left join [user_profile] c on s.employee_code = c.user_id "
					+ " where  s.employee_code is not null and  s.employee_code <> '' and role_code like '%L1' and status <> 'Inactive' "; 
			int arrSize = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject())) {
				qry = qry + " and s.project = ? ";
				arrSize++;
			}
			
			  if(!StringUtils.isEmpty(obj) &&
			  !StringUtils.isEmpty(obj.getDepartment_code())) { qry = qry +
			  " and s.department_code = ? "; arrSize++; }
			 
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				qry = qry + " and s.safety_type LIKE '%"+obj.getIncident_type()+"%' ";
				//arrSize++;
			}
		
			qry = qry + " order by   s.employee_code asc";
			Object[] pValues = new Object[arrSize];
			int i = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject())) {
				pValues[i++] = obj.getProject();
			}
			
			  if(!StringUtils.isEmpty(obj) &&
			 !StringUtils.isEmpty(obj.getDepartment_code())) { pValues[i++] =
			  obj.getDepartment_code(); }
			 
			/*
			 * if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type()))
			 * { pValues[i++] = obj.getIncident_type(); }
			 */
	
			objsList = jdbcTemplate.query( qry, pValues, new BeanPropertyRowMapper<RoleMapping>(RoleMapping.class));
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		return objsList;
	}
	static UrlGenerator ugObj = new UrlGenerator();
	public String getUniqueID(IRM cObj) throws Exception {
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet resultSet = null;
		String u_id = null;
		try{
			con = dataSource.getConnection();
			String contract_updateQry = " SELECT ISNULL(MAX(id +1), 0 +1) as maxId FROM [safety_ims] ";
			stmt = con.prepareStatement(contract_updateQry);
			resultSet = stmt.executeQuery();
			while(resultSet.next()) {
				u_id = resultSet.getString("maxId");
			}
		}catch(Exception e){ 
			e.printStackTrace();
			throw new Exception(e);
		}finally {
			DBConnectionHandler.closeJDBCResoucrs(con, stmt, resultSet);
		}	
		return u_id;
		
	}
	public boolean irmSubmit(IRM obj) throws Exception {
		int count = 0;
		RoleMapping role = new RoleMapping();
		role.setProject(obj.getProject_code());
		role.setDepartment_code(obj.getDepartment_code());
		role.setSafety_type(obj.getIncident_type());
		List<RoleMapping> objsList = new ArrayList<RoleMapping>();
		boolean flag = false;
		TransactionDefinition def = new DefaultTransactionDefinition();
		TransactionStatus status = transactionManager.getTransaction(def);
		//Calendar now = Calendar.getInstance();
	   // DateFormat df = new SimpleDateFormat("_yyMM_");
	   // String result = df.format(now.getTime());
		// String document_no = result;
		try {
			obj.setRisk_type("Medium");
			String file_name = "";
		//	String u_id = getUniqueID(obj);
			//obj.setDocument_code("IRM"+document_no+u_id);
			obj.setStatus("In Progress");
			if(StringUtils.isEmpty(obj.getApprover_code())) {
				obj.setStatus(null);
			}
			if(!StringUtils.isEmpty(obj.getMediaList())) {
			for (int i = 0; i < (obj.getMediaList().length); i++) {
				MultipartFile multipartFile = obj.getMediaList()[i];
				if (null != multipartFile && !multipartFile.isEmpty()) {
					String saveDirectory = CommonConstants.SAFETY_FILE_SAVING_PATH + obj.getDocument_code() + File.separator;
					String fileName = multipartFile.getOriginalFilename();
					//obj.setCreated_date(DateParser.parse(date));
					if (null != multipartFile && !multipartFile.isEmpty()) {
						FileUploads.singleFileSaving(multipartFile, saveDirectory, fileName);
					}
					file_name = file_name+ fileName+",";
				}
			}
			}
			if(!StringUtils.isEmpty(obj.getImage_list())) {
				for (int i = 0; i < (obj.getImage_list().length); i++) {
					
					    byte[] decodedBytes = Base64.getDecoder().decode(obj.getImage_list()[i]);
				        byte[] pdfBytes = decodedBytes; 
				        String saveDirectory = CommonConstants.SAFETY_FILE_SAVING_PATH + obj.getDocument_code() + File.separator + File.separator;
				        File directory = new File(saveDirectory);
		                if (!directory.exists()) {
		                	directory.mkdirs();
		                	boolean flag1 = OSValidator.isUnix();
		                	if(flag1) {
		                    	String perm = "rwxrwxrwx";
		    	            	Set<PosixFilePermission> permissions = PosixFilePermissions.fromString(perm);
		    	            	Files.setPosixFilePermissions(directory.toPath(), permissions);
		                	}
		                }
				        Files.write(Paths.get( saveDirectory+obj.getFilenameAndExtList()[i]), pdfBytes);
				        file_name = file_name+ obj.getFilenameAndExtList()[i]+",";
				}
			}
			if(!StringUtils.isEmpty(file_name)) {
				StringBuilder builder = new StringBuilder(file_name);
				int lastindex = file_name.lastIndexOf(",");
				builder.replace(lastindex, lastindex + 1, "" );
				file_name = builder.toString();
				obj.setPhoto(file_name);
			}
			NamedParameterJdbcTemplate namedParamJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
			String insertQry = "INSERT INTO [safety_ims] "
								+ "(document_code,incident_type,description,person_location,department_code,project_code,photo,location,ptw_code,risk_type,status,created_by,created_date)"
								+ " VALUES "
								+ "(:document_code,:incident_type,:description,:person_location,:department_code,:project_code,:photo,:location,:ptw_code,:risk_type,:status,:created_by,getdate())";
			BeanPropertySqlParameterSource paramSource = new BeanPropertySqlParameterSource(obj);		 
		    count = namedParamJdbcTemplate.update(insertQry, paramSource);
			if(count > 0) {
				String rewardQry = "update user_profile set reward_points = COALESCE(reward_points, 0) + 10 "
						+ " where user_id= :created_by ";
					 paramSource = new BeanPropertySqlParameterSource(obj);		 
				    count = namedParamJdbcTemplate.update(rewardQry, paramSource);
				    obj.setAction("Incident Report");
				    String HIS_qry = "INSERT INTO [rewards_history] (action,	reward_points,	user_id,created_date) VALUES (:action,10,:created_by,getdate())";
			    	paramSource = new BeanPropertySqlParameterSource(obj);		 
				    count = namedParamJdbcTemplate.update(HIS_qry, paramSource);
				    
				    
				if(!StringUtils.isEmpty(obj.getApprover_code()) ) {
					EMailSender emailSender = new EMailSender();
					String link_url =CommonConstants.HOST+"/reirm/update-irm-form/" ;
					Mail mail = new Mail();
					mail.setMailTo(obj.getEmail());
					mail.setMailSubject("Incident - Report | Safety Alerts | Re Sustainability");
					String body = "Hi, <br>"
							+ " Greetings From <b>Re Sustainability | AAYUSH</b>"
							+ "<br> An <b>"+obj.getIncident_name()+" Report</b> has been Registered with <b>ID : "+obj.getDocument_code()+"</b>"
							+ "<br> For more info about the Status of the "+obj.getIncident_name()+" <br> Please follow the link  <a href="+link_url+obj.getDocument_code()+"><button>Click Here</button></a>"
							+ "<br><br><br>"
							+ "Thanks"
							+ "<p style='color : red'><b>AAYUSH | Safety First</b></p>"
							+ "<b>Re Sustainability</b>";
					String subject = "Acknowledgment!";
					emailSender.send(mail.getMailTo(), mail.getMailSubject(), body,obj,subject);
					
					//*************************  Approvers List Start ***********************************
					 objsList = getRoleMappedOrNot(role);
					 int evauatorsCOunt = objsList.size() -1;
					 int c = 0;
					 if(!StringUtils.isEmpty(objsList) && objsList.size() > 0 && evauatorsCOunt > c) {
						 for (RoleMapping level2 : objsList) {
							 c++;
							 emailSender = new EMailSender();
							 mail = new Mail();
							 mail.setMailTo(level2.getEmail_id());
							 mail.setMailSubject("Incident - Report | Safety Alerts | Re Sustainability");
							 body = "Hi, <br>"
										+ " Greetings From <b>Re Sustainability | AAYUSH</b>"
										+ "<br> An <b>"+obj.getIncident_name()+" Report</b> has been Registered with <b>ID : "+obj.getDocument_code()+"</b>"
										+ "<br> For more info about the Status of the "+obj.getIncident_name()+" <br> Please follow the link  <a href="+link_url+obj.getDocument_code()+"><button>Click Here</button></a>"
										+ "<br><br><br>"
										+ "Thanks"
										+ "<p style='color : red'><b>AAYUSH | Safety First</b></p>"
										+ "<b>Re Sustainability</b>";
								subject = "Safety Registered - Notification!";
								emailSender.send(mail.getMailTo(), mail.getMailSubject(), body,obj,subject);
							 
						 }
					 }
					//*************************  Approvers List End ***********************************
					 if(!StringUtils.isEmpty(obj.getEmail_id()) ) {
						 java.util.Date date = new java.util.Date();    
						 emailSender = new EMailSender();
						 mail = new Mail();
						 mail.setMailTo(obj.getEmail_id());
						 mail.setMailSubject("Incident - Report | Safety Alerts | Re Sustainability");
						 body = "Hi, <br>"
									+ " Greetings From <b>Re Sustainability | AAYUSH</b>"
									+ "<br> An <b>"+obj.getIncident_name()+" Report</b> has been Reported on : "+date+" with <b>ID : "+obj.getDocument_code()+"</b>"
											+ " has come for your Review and the Resolution."
											+ "<p>Incident Details</p> "
											+ "<p>Project : "+obj.getProject_name()+"</p>"
											+ "<p>Raised by : "+obj.getEmail()+"</p>"
											+ "<p><b>Title </b>: "+obj.getIncident_name()+" &nbsp;|&nbsp; <b>Risk Type :</b> "+obj.getRisk_type()+"</p>"
											+ "<p><b>Description :</b> "+obj.getDescription()+" &nbsp;|&nbsp; <b>Category :</b> ("+obj.getIncident_category()+")</p>"
									+ "<br> For more info about the Status of the "+obj.getIncident_name()+" <br> Please follow the link  <a href="+link_url+obj.getDocument_code()+"><button>Click Here</button></a>"
									+ "<br><br><br>"
									+ "Thanks"
									+ "<p style='color : red'><b>AAYUSH | Safety First</b></p>"
									+ "<b>Re Sustainability</b>";
							subject = "Safety "+obj.getIncident_type()+" | Action";
							emailSender.send(mail.getMailTo(), mail.getMailSubject(), body,obj,subject);
					 }
					 
				}
				String insertQryForWorkFlow = "INSERT INTO [safety_ims_workflow] "
						+ "(document_no,approver_type,approver_code,status,action_taken,assigned_on,incident_category,level_status)"
						+ " VALUES "
						+ "(:document_code,:approver_type,:approver_code,:status,:action_taken,getdate(),:incident_category,:status)";
					 paramSource = new BeanPropertySqlParameterSource(obj);		 
				   int count2 = namedParamJdbcTemplate.update(insertQryForWorkFlow, paramSource);
				   if(count2 > 0) {
						flag = true;
						obj.setModule_type("IRM");
						obj.setMessage(obj.getDocument_code() +" - Incident Created Successfully");
						String logQry = "INSERT INTO [user_audit_log] "
								+ "(module_type,message,user_id,create_date)"
								+ " VALUES "
								+ "(:module_type,:message,:created_by,getdate())";
			 paramSource = new BeanPropertySqlParameterSource(obj);		 
		     count = namedParamJdbcTemplate.update(logQry, paramSource);
				   }
			}
			transactionManager.commit(status);
		}catch (Exception e) {
			transactionManager.rollback(status);
			e.printStackTrace();
			throw new Exception(e);
		}
		return flag;
	}

	public List<IRM> getIRMList(IRM obj) throws Exception  {
		List<IRM> objsList = null;
		try {
			int arrSize = 0;
			String qry =" select c.id,c.document_code,(select count(*) from [safety_ims] where status is null) as noCounts,(select count(*) from [safety_ims] where status = 'In Progress' ";
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
						qry = qry + " and CONVERT(date, created_date)  BETWEEN ? and ?  ";
						arrSize++;
						arrSize++;
					}else {
						//qry = qry + "and DATEDIFF(day,created_date,GETDATE()) between  0 and 30 ";
					}
					qry = qry + ") as counts,FORMAT(action_taken, 'dd-MMM-yy  HH:mm') as action_taken,incident_category,"
					+ "(select max(role_code) from role_mapping where project = c.project_code and [department_code] = d.[department_code] and [safety_type] = c.[incident_type] ) as maxRole,"
					+ "(select max(approver_type) from safety_ims_workflow where status = 'In Progress' and safety_ims_workflow.document_no = c.document_code) as maxRole2,";
					qry = qry +" (select count( distinct c.document_code) from safety_ims c left join [safety_ims_workflow] up on c.document_code = up.document_no  left join [project] p on c.project_code = p.project_code left join [sbu] sb on p.sbu_code = sb.sbu_code where c.incident_type is not null   ";
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
						qry = qry + " and p.sbu_code = ? ";
						arrSize++;
					}
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
						qry = qry + " and c.project_code = ? ";
						arrSize++;
					}
					
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
						qry = qry + " and  c.status = ? ";
						arrSize++;
					}
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
						qry = qry + " and  c.incident_type = ? ";
						arrSize++;
					}
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
						qry = qry + " and CONVERT(date, c.created_date)  BETWEEN ? and ?  ";
						arrSize++;
						arrSize++;
					}
					else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
						qry = qry + " and  CONVERT(date, c.created_date) = ? ";
						arrSize++;
					}
					else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
						qry = qry + " and  CONVERT(date, c.created_date) = ? ";
						arrSize++;
					}
					if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
						qry = qry + " and ( c.created_by = ? or approver_code in(select distinct approver_code from [safety_ims_workflow] where approver_code = ? ))";
						arrSize++;	arrSize++;
					}
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
						qry = qry + " and c.created_by = ?";
						arrSize++;	
					}
					if(!StringUtils.isEmpty(obj) && StringUtils.isEmpty(obj.getFrom_date())) {
						//qry = qry + " and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30 ";
					}
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
						qry = qry + " and  c.status = ?  ";
						arrSize++;
					}

					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed()) ) {
						qry = qry + " and  c.status = ? and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
						arrSize++;
					}
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
						qry = qry + " and  c.status = ? and  (up.approver_code = ? or  c.created_by = ?) and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
						arrSize++;
						arrSize++;arrSize++;
					}
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_no_reviewer())) {
						qry = qry + " and  c.status is null ";
					}
					qry = qry +  " ) as all_irm ,"
							+ "  (select count(*) from [safety_ims] c "
							+ " left join [project] p on c.project_code = p.project_code "
							+ "left join [sbu] sb on p.sbu_code = sb.sbu_code where c.[status] is null";
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
								qry = qry + " and p.sbu_code = ? ";
								arrSize++;
							}
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
								qry = qry + " and c.project_code = ? ";
								arrSize++;
							}
			
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
								qry = qry + "  and c.status = ? ";
								arrSize++;
							}
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
								qry = qry + " and c.incident_type = ? ";
								arrSize++;
							}
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
								qry = qry + " and CONVERT(date, c.created_date)  BETWEEN ? and ?  ";
								arrSize++;
								arrSize++;
							}
							else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
								qry = qry + " and  CONVERT(date, c.created_date) = ? ";
								arrSize++;
							}
							else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
								qry = qry + " and  CONVERT(date, c.created_date) = ? ";
								arrSize++;
							}
							if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
								qry = qry + " and ( c.created_by = ? or approver_code in(select distinct approver_code from [safety_ims_workflow] where approver_code = ? ))";

								arrSize++;	arrSize++;
							}
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
								qry = qry + " and c.created_by = ? ";
								arrSize++;	
							}
							if(!StringUtils.isEmpty(obj) && StringUtils.isEmpty(obj.getI_pending())) {
								//qry = qry + " and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30 ";
							}
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
								qry = qry + " and  c.status = ? ";
								arrSize++;
							}
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
								qry = qry + " and  c.status = ? and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
								arrSize++;
							}

							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
								qry = qry + " and  c.status = ? and  (up.approver_code = ? or  c.created_by = ?) and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
								arrSize++;
								arrSize++;arrSize++;
							}
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_no_reviewer())) {
								qry = qry + " and  c.status is null ";
							}
								
							qry = qry + " ) as not_assigned,";
							qry = qry +	"(select count( c.[document_code]) from [safety_ims] c left join [project] p on c.project_code = p.project_code left join [sbu] sb on p.sbu_code = sb.sbu_code where c.[document_code] is not null and c.status ='Resolved'  ";
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
										qry = qry + " and p.sbu_code = ? ";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
										qry = qry + " and c.project_code = ? ";
										arrSize++;
									}
					
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
										qry = qry + "  and c.status = ? ";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
										qry = qry + " and c.incident_type = ? ";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
										qry = qry + " and CONVERT(date, c.created_date)  BETWEEN ? and ?  ";
										arrSize++;
										arrSize++;
									}
									else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
										qry = qry + " and  CONVERT(date, c.created_date) = ? ";
										arrSize++;
									}
									else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
										qry = qry + " and  CONVERT(date, c.created_date) = ? ";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
										qry = qry + " and ( c.created_by = ? or approver_code in(select distinct approver_code from [safety_ims_workflow] where approver_code = ? ))";
										arrSize++;
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
										qry = qry + " and c.created_by = ?";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && StringUtils.isEmpty(obj.getI_pending())) {
										//qry = qry + " and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30 ";
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
										qry = qry + " and  c.status = ? ";
										arrSize++;
									}

									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed()) ) {
										qry = qry + " and  c.status = ? and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
										qry = qry + " and  c.status = ? and  (up.approver_code = ? or  c.created_by = ?) and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
										arrSize++;
										arrSize++;arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_no_reviewer())) {
										qry = qry + " and  c.status is null ";
									}
										
									qry = qry + " ) as active_irm,"
									+ "(select count( c.[document_code]) from [safety_ims] c left join [project] p on c.project_code = p.project_code left join [sbu] sb on p.sbu_code = sb.sbu_code where c.[document_code] is not null and c.status ='In Progress' ";
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
										qry = qry + " and p.sbu_code = ? ";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
										qry = qry + " and c.project_code = ? ";
										arrSize++;
									}
									
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
										qry = qry + " and  c.status = ? ";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
										qry = qry + " and  c.incident_type = ? ";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
										qry = qry + " and CONVERT(date, c.created_date)  BETWEEN ? and ?  ";
										arrSize++;
										arrSize++;
									}
									else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
										qry = qry + " and  CONVERT(date, c.created_date) = ? ";
										arrSize++;
									}
									else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
										qry = qry + " and  CONVERT(date, c.created_date) = ? ";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
										qry = qry + " and ( c.created_by = ? or approver_code in(select distinct approver_code from [safety_ims_workflow] where approver_code = ? ))";
										arrSize++;
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
										qry = qry + " and c.created_by = ?";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && StringUtils.isEmpty(obj.getI_pending())) {
										qry = qry + " ";
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
										qry = qry + " and  c.status = ? ";
										arrSize++;
									}
				
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed()) ) {
										qry = qry + " and  c.status = ? and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
										qry = qry + " and  c.status = ? and  (up.approver_code = ? or  c.created_by = ?) and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
										arrSize++;
										arrSize++;arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_no_reviewer())) {
										qry = qry + " and  c.status is null ";
									}
									
										
									qry = qry + " ) as inActive_irm,"
					+ "rm.incident_code,rm.incident_type,approver_type,p.sbu_code,sb.sbu_name,	 c.project_code,p.project_name,c.risk_type,c.description,c.status,c.ptw_code,c.department_code,d.department_name,"
					+ "	l.location_code,l.location_name,c.created_by,u.email_id,u.user_name, COALESCE(up.approver_code, 'No Reviewer Assigneds') AS approver_code,u1.user_name as approver_name,FORMAT(c.created_date, 'dd-MMM-yy  HH:mm') as created_date from [safety_ims] c "
					+ " left join [safety_ims_workflow] up on c.document_code = up.document_no "
					+ " left join [project] p on c.project_code = p.project_code "
					+ " left join [sbu] sb on p.sbu_code = sb.sbu_code"
					+ " left join [department] d on c.department_code = d.department_code "
					+ " left join [project_location] l on c.location = l.location_code "
					+ " left join [incident] rm on c.incident_type = rm.incident_code "
					+ " left join [user_profile] u on c.created_by = u.user_id "
					+ " left join [user_profile] u1 on up.approver_code = u1.user_id "
					+ " where  document_code is not null   ";
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				qry = qry + " and p.sbu_code = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				qry = qry + " and c.project_code = ? ";
				arrSize++;
			}
									
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				qry = qry + " and c.incident_type = ?";
				arrSize++;
			}	
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				qry = qry + " and  c.status = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
				qry = qry + " and CONVERT(date, c.created_date)  BETWEEN ? and ?  ";
				arrSize++;
				arrSize++;
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
				qry = qry + " and  CONVERT(date, c.created_date) = ? ";
				arrSize++;
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
				qry = qry + " and  CONVERT(date, c.created_date) = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				qry = qry + " and ( c.created_by = ? or approver_code in(select distinct approver_code from [safety_ims_workflow] where approver_code = ? ))";
				arrSize++;
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
				qry = qry + " and c.created_by = ?";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && StringUtils.isEmpty(obj.getI_pending())) {
				//qry = qry + " and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30 ";
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				qry = qry + " and  up.status = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed()) ) {
				qry = qry + " and  c.status = ? and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				qry = qry + " and  c.status = ? and (up.approver_code = ? or  c.created_by = ?)  and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
				arrSize++;
				arrSize++;
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_no_reviewer())) {
				qry = qry + " and  c.status is null ";
			}
				
			if(StringUtils.isEmpty(obj.getPageIndex())) {
				obj.setPageIndex("0");
			}
			qry = qry + " order by FORMAT(c.created_date, 'dd-MMM-yy'),FORMAT(c.created_date, 'HH:mm'),approver_type desc  ";
			//qry = qry + " order by FORMAT(c.created_date, 'dd-MMM-yy'),FORMAT(c.created_date, 'HH:mm'),approver_type desc OFFSET "+obj.getPageIndex()+"0 ROWS FETCH NEXT 40 ROWS ONLY ";

			Object[] pValues = new Object[arrSize];
			int i = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getFrom_date();
				pValues[i++] = obj.getTo_date();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				pValues[i++] = obj.getSbu_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				pValues[i++] = obj.getProject_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				pValues[i++] = obj.getStatus();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				pValues[i++] = obj.getIncident_type();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getFrom_date();
				pValues[i++] = obj.getTo_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
				pValues[i++] = obj.getFrom_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getTo_date();
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getUser(); pValues[i++] = obj.getUser();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			pValues[i++] = obj.getAdmin_incidents();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				pValues[i++] = obj.getI_pending();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				pValues[i++] = obj.getI_completed();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getI_completed();
				pValues[i++] = obj.getUser();
				pValues[i++] = obj.getUser();
			}
			
			
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				pValues[i++] = obj.getSbu_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				pValues[i++] = obj.getProject_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				pValues[i++] = obj.getStatus();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				pValues[i++] = obj.getIncident_type();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getFrom_date();
				pValues[i++] = obj.getTo_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
				pValues[i++] = obj.getFrom_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getTo_date();
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getUser(); pValues[i++] = obj.getUser();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			pValues[i++] = obj.getAdmin_incidents();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				pValues[i++] = obj.getI_pending();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				pValues[i++] = obj.getI_completed();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getI_completed();
				pValues[i++] = obj.getUser();
				pValues[i++] = obj.getUser();
			}
			
			
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				pValues[i++] = obj.getSbu_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				pValues[i++] = obj.getProject_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				pValues[i++] = obj.getStatus();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				pValues[i++] = obj.getIncident_type();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getFrom_date();
				pValues[i++] = obj.getTo_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
				pValues[i++] = obj.getFrom_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getTo_date();
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getUser(); pValues[i++] = obj.getUser();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			pValues[i++] = obj.getAdmin_incidents();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				pValues[i++] = obj.getI_pending();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				pValues[i++] = obj.getI_completed();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getI_completed();
				pValues[i++] = obj.getUser();
				pValues[i++] = obj.getUser();
			}
				
			
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				pValues[i++] = obj.getSbu_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				pValues[i++] = obj.getProject_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				pValues[i++] = obj.getStatus();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				pValues[i++] = obj.getIncident_type();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getFrom_date();
				pValues[i++] = obj.getTo_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
				pValues[i++] = obj.getFrom_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getTo_date();
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getUser(); pValues[i++] = obj.getUser();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			pValues[i++] = obj.getAdmin_incidents();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				pValues[i++] = obj.getI_pending();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				pValues[i++] = obj.getI_completed();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getI_completed();
				pValues[i++] = obj.getUser();
				pValues[i++] = obj.getUser();
			}
			
				
			
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				pValues[i++] = obj.getSbu_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				pValues[i++] = obj.getProject_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				pValues[i++] = obj.getIncident_type();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				pValues[i++] = obj.getStatus();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getFrom_date();
				pValues[i++] = obj.getTo_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
				pValues[i++] = obj.getFrom_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getTo_date();
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getUser(); pValues[i++] = obj.getUser();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			pValues[i++] = obj.getAdmin_incidents();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				pValues[i++] = obj.getI_pending();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				pValues[i++] = obj.getI_completed();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getI_completed();
				pValues[i++] = obj.getUser();
				pValues[i++] = obj.getUser();
			}
			
				
			
			objsList = jdbcTemplate.query( qry,pValues, new BeanPropertyRowMapper<IRM>(IRM.class));
			
			Set<String> nameSet = new HashSet<>();
			objsList = objsList.stream()
		            .filter(e -> nameSet.add(e.getDocument_code()))
		            .collect(Collectors.toList());
		}catch(Exception e){ 
			e.printStackTrace();
			throw new Exception(e);
		}
		return objsList;
	}

	public List<IRM> getSBUFilterListFromIRM(IRM obj) throws Exception {
		List<IRM> objsList = new ArrayList<IRM>();
		try {
			String qry = "SELECT distinct p.sbu_code , sb.sbu_name FROM [safety_ims] s "
					+ " left join [safety_ims_workflow] up on s.document_code = up.document_no "
					+ " left join [incident] c on s.incident_type = c.incident_code "
					+ " left join [project] p on s.project_code = p.project_code left join [sbu] sb on p.sbu_code = sb.sbu_code"
					+ " where  p.sbu_code is not null and  p.sbu_code <> '' "; 
			int arrSize = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				qry = qry + " and p.sbu_code = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				qry = qry + " and s.project_code = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				qry = qry + " and s.status = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				qry = qry + " and s.incident_type = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
			qry = qry + " and ( s.created_by = ? or approver_code in(select distinct approver_code from [safety_ims_workflow] where approver_code = ? ))";
				arrSize++;	arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			qry = qry + " and  s.created_by = ? ";
				arrSize++;	
			}
			if(!StringUtils.isEmpty(obj) && StringUtils.isEmpty(obj.getI_pending())) {
				qry = qry + " and DATEDIFF(day,s.created_date,GETDATE()) between  0 and 30 ";
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				qry = qry + " and  s.status = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				qry = qry + " and  s.status = ? ";
				arrSize++;
			}
			Object[] pValues = new Object[arrSize];
			int i = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				pValues[i++] = obj.getSbu_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				pValues[i++] = obj.getProject_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				pValues[i++] = obj.getIncident_type();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				pValues[i++] = obj.getStatus();
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getUser(); pValues[i++] = obj.getUser();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			pValues[i++] = obj.getAdmin_incidents();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				pValues[i++] = obj.getI_pending();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				pValues[i++] = obj.getI_completed();
			}
			objsList = jdbcTemplate.query( qry, pValues, new BeanPropertyRowMapper<IRM>(IRM.class));
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		return objsList;
	}

	public List<IRM> getProjectFilterListFromIRM(IRM obj) throws Exception {
		List<IRM> objsList = new ArrayList<IRM>();
		try {
			String qry = "SELECT distinct s.project_code , p.project_name FROM [safety_ims] s "
					+ " left join [safety_ims_workflow] up on s.document_code = up.document_no "
					+ " left join [incident] c on s.incident_type = c.incident_code "
					+ " left join [project] p on s.project_code = p.project_code"
					+ " left join [sbu] sb on p.sbu_code = sb.sbu_code"
					+ " where  s.incident_type is not null and  s.incident_type <> '' "; 
			int arrSize = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				qry = qry + " and p.sbu_code = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				qry = qry + " and s.project_code = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				qry = qry + " and s.status = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				qry = qry + " and s.incident_type = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
			qry = qry + " and ( s.created_by = ? or approver_code in(select distinct approver_code from [safety_ims_workflow] where approver_code = ? ))";
				arrSize++;	arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			qry = qry + " and s.created_by = ?";
				arrSize++;	
			}
			if(!StringUtils.isEmpty(obj) && StringUtils.isEmpty(obj.getI_pending())) {
				qry = qry + " and DATEDIFF(day,s.created_date,GETDATE()) between  0 and 30 ";
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				qry = qry + " and  s.status = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				qry = qry + " and  s.status = ? ";
				arrSize++;
			}
			Object[] pValues = new Object[arrSize];
			int i = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				pValues[i++] = obj.getSbu_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				pValues[i++] = obj.getProject_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				pValues[i++] = obj.getIncident_type();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				pValues[i++] = obj.getStatus();
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getUser(); pValues[i++] = obj.getUser();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			pValues[i++] = obj.getAdmin_incidents();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				pValues[i++] = obj.getI_pending();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				pValues[i++] = obj.getI_completed();
			}
			objsList = jdbcTemplate.query( qry, pValues, new BeanPropertyRowMapper<IRM>(IRM.class));
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		return objsList;
	}

	public List<IRM> getIncidentFilterListFromIRM(IRM obj) throws Exception {
		List<IRM> objsList = new ArrayList<IRM>();
		try {
			String qry = "SELECT distinct c.incident_code , c.incident_type FROM [safety_ims] s "
					+ " left join [safety_ims_workflow] up on s.document_code = up.document_no "
					+ " left join [incident] c on s.incident_type = c.incident_code "
					+ " left join [project] p on s.project_code = p.project_code"
					+ " left join [sbu] sb on p.sbu_code = sb.sbu_code"
					+ " where  s.incident_type is not null and  s.incident_type <> '' "; 
			int arrSize = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				qry = qry + " and p.sbu_code = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				qry = qry + " and s.project_code = ? ";
				arrSize++;
			}
			
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				qry = qry + " and s.status = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				qry = qry + " and s.incident_type = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
			qry = qry + " and ( s.created_by = ? or approver_code in(select distinct approver_code from [safety_ims_workflow] where approver_code = ? ))";
				arrSize++;	arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			qry = qry + " and s.created_by = ?";
				arrSize++;	
			}
			if(!StringUtils.isEmpty(obj) && StringUtils.isEmpty(obj.getI_pending())) {
				qry = qry + " and DATEDIFF(day,s.created_date,GETDATE()) between  0 and 30 ";
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				qry = qry + " and  s.status = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				qry = qry + " and  s.status = ? ";
				arrSize++;
			}
			Object[] pValues = new Object[arrSize];
			int i = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				pValues[i++] = obj.getSbu_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				pValues[i++] = obj.getProject_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				pValues[i++] = obj.getIncident_type();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				pValues[i++] = obj.getStatus();
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getUser(); pValues[i++] = obj.getUser();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			pValues[i++] = obj.getAdmin_incidents();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				pValues[i++] = obj.getI_pending();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				pValues[i++] = obj.getI_completed();
			}
			objsList = jdbcTemplate.query( qry, pValues, new BeanPropertyRowMapper<IRM>(IRM.class));
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		return objsList;
	}

	public List<IRM> getStatusFilterListFromIRM(IRM obj) throws Exception {
		List<IRM> objsList = new ArrayList<IRM>();
		try {
			String qry = "SELECT distinct s.status  FROM [safety_ims] s "
					+ " left join [safety_ims_workflow] up on s.document_code = up.document_no "
					+ " left join [project] p on s.project_code = p.project_code"
					+ " left join [sbu] sb on p.sbu_code = sb.sbu_code"
					+ " where  s.status is not null and  s.status <> '' "; 
			int arrSize = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				qry = qry + " and p.sbu_code = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				qry = qry + " and s.project_code = ? ";
				arrSize++;
			}
			
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				qry = qry + " and s.status = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				qry = qry + " and incident_type = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && ( !CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
			qry = qry + " and ( s.created_by = ? or approver_code in(select distinct approver_code from [safety_ims_workflow]   where approver_code = ? ))";
				arrSize++;	arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			qry = qry + " and s.created_by = ?";
				arrSize++;	
			}
			if(!StringUtils.isEmpty(obj) && StringUtils.isEmpty(obj.getI_pending())) {
				qry = qry + " and DATEDIFF(day,s.created_date,GETDATE()) between  0 and 30 ";
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				qry = qry + " and  s.status = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				qry = qry + " and  s.status = ? ";
				arrSize++;
			}
			qry = qry + " order by s.status asc";
			Object[] pValues = new Object[arrSize];
			int i = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				pValues[i++] = obj.getSbu_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				pValues[i++] = obj.getProject_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				pValues[i++] = obj.getIncident_type();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				pValues[i++] = obj.getStatus();
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getUser(); pValues[i++] = obj.getUser();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			pValues[i++] = obj.getAdmin_incidents();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				pValues[i++] = obj.getI_pending();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				pValues[i++] = obj.getI_completed();
			}
			objsList = jdbcTemplate.query( qry, pValues, new BeanPropertyRowMapper<IRM>(IRM.class));
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		return objsList;
	}

	public List<IRM> getProjectstListIRMUpdate(IRM irm) throws SQLException {
		List<IRM> menuList = null;
		try{  
			String qry = "select project_code,project_name from [project] where status <> 'Inactive' ";
			menuList = jdbcTemplate.query( qry, new BeanPropertyRowMapper<IRM>(IRM.class));
			
		}catch(Exception e){ 
			e.printStackTrace();
			throw new SQLException(e.getMessage());
		}
		return menuList;
	}

	public List<IRM> getDepartmentsIRMUpdate(IRM irm) throws SQLException {
		List<IRM> menuList = null;
		try{  
			String qry = "select department_code,department_name from [department] where status <> 'Inactive' ";
			menuList = jdbcTemplate.query( qry, new BeanPropertyRowMapper<IRM>(IRM.class));
			
		}catch(Exception e){ 
			e.printStackTrace();
			throw new SQLException(e.getMessage());
		}
		return menuList;
	}

	public List<IRM> getLocationstListIRMUpdate(IRM irm) throws SQLException {
		List<IRM> menuList = null;
		try{  
			String qry = "select location_code,location_name from [project_location] where status <> 'Inactive' ";
			menuList = jdbcTemplate.query( qry, new BeanPropertyRowMapper<IRM>(IRM.class));
			
		}catch(Exception e){ 
			e.printStackTrace();
			throw new SQLException(e.getMessage());
		}
		return menuList;
	}

	public IRM getIRMDocumentDEtails(IRM irm) throws Exception  {
		IRM obj = null;
		try {
			String qry = "select TOP(1) rm.incident_code,i.incident_type,approver_type,u1.email_id,p.sbu_code,up.incident_category,	c.document_code, c.project_code,p.project_name,c.risk_type,c.description,up.status,c.status as mainStatus,c.ptw_code,c.department_code,d.department_name,"
					+"	l.location_code,l.location_name,c.created_by,u.user_name, up.approver_code,u1.user_name as approver_name,"
					+ "FORMAT(up.action_taken, 'dd-MMM-yy  HH:mm') as action_taken,FORMAT(c.created_date, 'dd-MMM-yy  HH:mm') as created_date,photo from [safety_ims] c "
					+" left join [safety_ims_workflow] up on c.document_code = up.document_no "
					+" left join [project] p on c.project_code = p.project_code "
					+ " left join [sbu] sb on p.sbu_code = sb.sbu_code"
					+" left join [department] d on c.department_code = d.department_code "
					+" left join [project_location] l on c.location = l.location_code "
					+" left join [incident] rm on c.incident_type = rm.incident_code "
					+" left join [incident] i on c.incident_type = i.incident_code "
					+" left join [user_profile] u on c.created_by = u.user_id "
					+" left join [user_profile] u1 on up.approver_code = u1.user_id "
					+" where  document_code is not null  " ; 
			int arrSize = 0;
			if(!StringUtils.isEmpty(irm) && !StringUtils.isEmpty(irm.getDocument_code())) {
				qry = qry + " and c.document_code = ? ";
				arrSize++;
			}
			qry = qry + "order by approver_type desc";
			Object[] pValues = new Object[arrSize];
			int i = 0;
			if(!StringUtils.isEmpty(irm) && !StringUtils.isEmpty(irm.getDocument_code())) {
				pValues[i++] = irm.getDocument_code();
			}
			obj = (IRM)jdbcTemplate.queryForObject(qry, pValues, new BeanPropertyRowMapper<IRM>(IRM.class));
					//(IRM)jdbcTemplate.queryForObject(qry, pValues, new BeanPropertyRowMapper<IRM>(IRM.class));
			if(!StringUtils.isEmpty(irm) && !StringUtils.isEmpty(irm.getDocument_code())) {
				List<IRM> objsList = null;
				String qryDetails = "select distinct w.approver_type,r.id as rca_id,investigation_team,r.incident_type,level_status,incident_seviourity,ua_type,uc_type,management_type, document_no as document_code,w.approver_code,incident_category,w.status,FORMAT(w.assigned_on, 'dd-MMM-yy  HH:mm') as assigned_on,sb_notes,"
						+ "FORMAT(w.action_taken, 'dd-MMM-yy  HH:mm') as action_taken,FORMAT(w.sb_date, 'dd-MMM-yy  HH:mm') as sb_date,u.user_name as approver_name,u.email_id,w.corrective_action,w.preventive_action,w.remarks,w.attachment from safety_ims_workflow w "
						+ " left join [safety_ims] s on w.document_no = s.document_code "
						+ " left join rca r on w.document_no = r.document_code  "
						+ "left join [incident] rm on s.incident_type = rm.incident_code  "
						+ "left join [user_profile] u on w.approver_code = u.user_id where w.document_no = ? and w.status in('Reviewed','In Progress')  and r.status <> 'Inactive'  "
						+ "order by w.approver_type asc ";
				
				objsList = jdbcTemplate.query(qryDetails, new Object[] {irm.getDocument_code()}, new BeanPropertyRowMapper<IRM>(IRM.class));	
				if(objsList.size() == 0) {
					String qryDetails2 = "select distinct w.approver_type,r.id as rca_id,investigation_team,r.incident_type,incident_seviourity,ua_type,uc_type,level_status,management_type, document_no as document_code,w.approver_code,incident_category,w.status,FORMAT(w.assigned_on, 'dd-MMM-yy  HH:mm') as assigned_on,sb_notes,"
							+ "FORMAT(w.action_taken, 'dd-MMM-yy  HH:mm') as action_taken,FORMAT(w.sb_date, 'dd-MMM-yy  HH:mm') as sb_date,u.user_name as approver_name,u.email_id,w.corrective_action,w.preventive_action,w.remarks,w.attachment from safety_ims_workflow w "
							+ " left join [safety_ims] s on w.document_no = s.document_code "
							+ " left join rca r on w.document_no = r.document_code  "
							+ "left join [incident] rm on s.incident_type = rm.incident_code  "
							+ "left join [user_profile] u on w.approver_code = u.user_id where w.document_no = ? and w.status in('Reviewed','In Progress')  "
							+ "order by w.approver_type asc ";
					
					objsList = jdbcTemplate.query(qryDetails2, new Object[] {irm.getDocument_code()}, new BeanPropertyRowMapper<IRM>(IRM.class));
				}
				obj.setIrmIncidentsList(objsList);
				List<IRM> objsList2 = null;
				for(IRM details : obj.getIrmIncidentsList()) {
					String qryCAPADetails = "select w.id as capa_id,w.document_code,ca,pa,w.person_responsible,u.user_name, "
							+ " FORMAT(w.tentative_date, 'yyyy-MM-dd') as tentative_date,remarks,attachments as attachment from [capa] w "
							+ "left join [user_profile] u on w.person_responsible = u.user_id "
							+ " where w.document_code = ? and w.status <> 'Inactive' ";
					
					objsList2 = jdbcTemplate.query(qryCAPADetails, new Object[] {irm.getDocument_code()}, new BeanPropertyRowMapper<IRM>(IRM.class));	
					details.setCapaList(objsList2);
				}
				List<IRM> objsList1 = null;
				String qryRoleDetails = "select distinct role_code,employee_code,u.email_id,u.user_id,u.user_name as next_level_user from [role_mapping] w "
						+ " left join [safety_ims] s on w.safety_type = s.incident_type "
						+ "left join [user_profile] u on w.employee_code = u.user_id "
						+ " where w.project = ?  and w.status <> 'Inactive' and role_code not like '%L1' ";
				
				objsList1 = jdbcTemplate.query(qryRoleDetails, new Object[] {obj.getProject_code()}, new BeanPropertyRowMapper<IRM>(IRM.class));	

				//objsList1 = jdbcTemplate.query(qryRoleDetails, new Object[] {obj.getProject_code(),obj.getDepartment_code(),obj.getIncident_code()}, new BeanPropertyRowMapper<IRM>(IRM.class));	
				obj.setIrmRolesList(objsList1);
			}
		}catch(Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		return obj;
	}
	 private static String removeLastComma(String inputString) {
	        // Check if the string contains a comma
		 if (!inputString.isEmpty() && inputString.contains(",")) {
	            // Remove the first comma
	            //inputString = inputString.substring(1);

	            // Remove the last comma
	            int lastCommaIndex = inputString.lastIndexOf(",");
	            inputString = inputString.substring(0, lastCommaIndex) + inputString.substring(lastCommaIndex + 1);

	            System.out.println("Original string: ," + inputString);
	            System.out.println("String without first and last comma: " + inputString);
	        } 
	        // Return the original string if no comma is found
	        return inputString;
	    }
	public boolean irmUpdateSubmit(IRM obj) throws Exception {
		int count = 0;
		boolean flag = false;
		TransactionDefinition def = new DefaultTransactionDefinition();
		TransactionStatus status = transactionManager.getTransaction(def);
		try {
			String file_name = "";
			obj.setStatusChanged("Reviewed");
			if(!StringUtils.isEmpty(obj.getSb_notes())) {
				obj.setStatusChanged("Send Back");
			}
			
			
			NamedParameterJdbcTemplate namedParamJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
///////////////////////////// Send back Condition //////////////////////////////////////////////
			if(!StringUtils.isEmpty(obj.getSb_notes())) {
				String updateQry = "Update [safety_ims_workflow] set "
						+ " status= :statusChanged "
						+ "where document_no = :document_code and approver_type= :type_prevs and approver_code= :approver_prvs ";
					BeanPropertySqlParameterSource paramSource = new BeanPropertySqlParameterSource(obj);		 
					count = namedParamJdbcTemplate.update(updateQry, paramSource);
					obj.setStatusChanged("Sent Back");
					 String fileName  = "";
					 if(!StringUtils.isEmpty(obj.getDocs())) {
					    	for(int i =0; i < obj.getDocs().length; i++) {
						    	fileName = fileName+","+obj.getDocs()[i]+",";
								fileName = removeLastComma(fileName);
								obj.setAttachment(fileName);
						    }
				    	}
					    for(int i =0; i < obj.getMediaList().length; i++) {
					    	MultipartFile multipartFile = obj.getMediaList()[i];
							if (null != multipartFile && !multipartFile.isEmpty() || !StringUtils.isEmpty(obj.getDocs()) && obj.getDocs().length > 0) {
								if(null != multipartFile && !multipartFile.isEmpty()) {
									String saveDirectory = CommonConstants.SAFETY_FILE_SAVING_PATH + obj.getDocument_code() + File.separator+ obj.getApprover_type_name() + File.separator;
									fileName = multipartFile.getOriginalFilename();
									if (null != multipartFile && !multipartFile.isEmpty()) {
										FileUploads.singleFileSaving(multipartFile, saveDirectory, fileName);
									}
								}else {
									fileName = fileName+obj.getDocs()[i]+",";
									fileName = removeLastComma(fileName);
								}
								obj.setAttachment(fileName);
							}
					    }
					String updateQryNext = "Update [safety_ims_workflow] set "
							+ " status= :statusChanged,sb_date= getdate(),corrective_action= :ca_before,level_status= :level_status,"
							+ "preventive_action= :pa_before,remarks= :remarks_before,attachment= :doc_before,sb_notes= :sb_notes "
							+ "where document_no = :document_code and approver_type= :approver_type_before and approver_code= :employee_code_before and status = 'In Progress' ";
						paramSource = new BeanPropertySqlParameterSource(obj);		 
						count = namedParamJdbcTemplate.update(updateQryNext, paramSource);
					
					if(count > 0 && !StringUtils.isEmpty(obj.getEmployee_code_before()) ) {
					
						obj.setStatus("In Progress");
						String insertQry = "INSERT INTO [safety_ims_workflow] "
											+ "(document_no,approver_type,approver_code,incident_category,status,assigned_on,corrective_action,preventive_action,remarks,attachment,level_status,sb_notes,sb_date)"
											+ " VALUES "
											+ "(:document_code,:type_prevs,:approver_prvs,:incident_category,:status,getdate(),:ca_before,:pa_before,:remarks_before,:doc_before,:status,:sb_notes,getdate())";
						paramSource = new BeanPropertySqlParameterSource(obj);		 
					    count = namedParamJdbcTemplate.update(insertQry, paramSource);
					     fileName  = null;
					    if(!StringUtils.isEmpty(obj.getCas())) {
					    	int arraySize = 0;
					    	if(!StringUtils.isEmpty(obj.getRemarkss()) && obj.getRemarkss().length > 0) {
								obj.setRemarkss(CommonMethods.replaceEmptyByNullInSringArray(obj.getRemarkss()));
								if(arraySize < obj.getRemarkss().length) {
									arraySize = obj.getRemarkss().length;
								}
							}
					    	if(!StringUtils.isEmpty(obj.getCas()) && obj.getCas().length > 0) {
								obj.setCas(CommonMethods.replaceEmptyByNullInSringArray(obj.getCas()));
								if(arraySize < obj.getCas().length) {
									arraySize = obj.getCas().length;
								}
							}
					    	if(!StringUtils.isEmpty(obj.getPas()) && obj.getPas().length > 0) {
								obj.setPas(CommonMethods.replaceEmptyByNullInSringArray(obj.getPas()));
								if(arraySize < obj.getPas().length) {
									arraySize = obj.getPas().length;
								}
							}
					    	if(!StringUtils.isEmpty(obj.getDocs())) {
						    	for(int i =0; i < obj.getDocs().length; i++) {
							    	fileName = fileName+","+obj.getDocs()[i]+",";
									fileName = removeLastComma(fileName);
									obj.setAttachment(fileName);
							    }
					    	}
					    for(int i =0; i < obj.getCas().length; i++) {
					    	MultipartFile multipartFile = obj.getMediaList()[i];
							if (null != multipartFile && !multipartFile.isEmpty() || !StringUtils.isEmpty(obj.getDocs()) &&  obj.getDocs().length > 0) {
								if(null != multipartFile && !multipartFile.isEmpty()) {
									String saveDirectory = CommonConstants.SAFETY_FILE_SAVING_PATH + obj.getDocument_code() + File.separator+ obj.getApprover_type_name() + File.separator;
									fileName = multipartFile.getOriginalFilename();
									if (null != multipartFile && !multipartFile.isEmpty()) {
										FileUploads.singleFileSaving(multipartFile, saveDirectory, fileName);
									}
								}else {
									fileName = fileName+obj.getDocs()[i]+",";
									fileName = removeLastComma(fileName);
								}
								obj.setAttachment(fileName);
							}
							obj.setCa((!StringUtils.isEmpty(obj.getCas()[i])?obj.getCas()[i]:null));
				    		obj.setPa((!StringUtils.isEmpty(obj.getPas()[i])?obj.getPas()[i]:null));
				    		obj.setRemarks((!StringUtils.isEmpty(obj.getRemarkss()[i])?obj.getRemarkss()[i]:null));
					    	obj.setTentative_date(obj.getTentative_dates()[i]);
					    	if(!StringUtils.isEmpty(obj.getCa()) && !StringUtils.isEmpty(obj.getPa())) {
					    		String ca_pa_insertQry = "INSERT INTO [capa] "
							    		+ "(document_code,ca,pa,person_responsible,tentative_date,remarks,attachments,status)"
										+ " VALUES "
										+ "(:document_code,:ca,:pa,:employee_code_before,:tentative_date,:remarks,:attachment,:status)";
								paramSource = new BeanPropertySqlParameterSource(obj);		 
							    count = namedParamJdbcTemplate.update(ca_pa_insertQry, paramSource);
					    	}
					    }
					    String updatercaQryNext = "Update [rca] set "
								+ " incident_type= :incident_type,investigation_team= :investigation_team,incident_seviourity= :incident_seviourity,ua_type= :ua_type,uc_type= :uc_type,management_type= :management_type "
								+ "where document_code = :document_code and id= :rca_id ";
							paramSource = new BeanPropertySqlParameterSource(obj);		 
							count = namedParamJdbcTemplate.update(updatercaQryNext, paramSource);
					}
				}
			}else {
				///////////////////////////// UPDATING LEVEL ! //////////////////////////////////////////////
				if(!StringUtils.isEmpty(obj) && !(obj.getLevel_code().equals("IRL1"))) {
					 String fileName  = "";
					 String file_Name  = "";
					    for(int i =0; i < obj.getMediaList().length; i++) {
					    	MultipartFile multipartFile = obj.getMediaList()[i];
							if (null != multipartFile && !multipartFile.isEmpty() || !StringUtils.isEmpty(obj.getDocs()) && obj.getDocs().length > 0) {
								if(null != multipartFile && !multipartFile.isEmpty()) {
									String saveDirectory = CommonConstants.SAFETY_FILE_SAVING_PATH + obj.getDocument_code() + File.separator+ obj.getApprover_type_name() + File.separator;
									fileName = multipartFile.getOriginalFilename();
									
									if (null != multipartFile && !multipartFile.isEmpty()) {
										FileUploads.singleFileSaving(multipartFile, saveDirectory, fileName);
									}
								}
								file_Name = file_Name+fileName+",";
								//file_Name = removeLastComma(file_Name);
								fileName = file_Name;
								obj.setAttachment(removeLastComma(fileName));
							}
					    }
					    if(!StringUtils.isEmpty(obj.getDocs())) {
						    for(int i =0; i < obj.getDocs().length; i++) {
						    	fileName = fileName+","+obj.getDocs()[i]+",";
								obj.setAttachment(removeLastComma(fileName));
						    }
					    }
						if(!(obj.getLevel_status().equals("Closed"))) {
							obj.setStatusChanged("In Progress");
						}
					String updateQry = "Update [safety_ims_workflow] set "
							+ " status= :statusChanged,action_taken= getdate(),corrective_action= :corrective_action,level_status= :level_status,"
							+ "preventive_action= :preventive_action,remarks= :remarks,attachment= :attachment "
							+ "where document_no = :document_code and approver_type= :approver_type_before and approver_code= :employee_code_before and status <> 'Send Back' and status <> 'Sent Back' ";
						BeanPropertySqlParameterSource paramSource = new BeanPropertySqlParameterSource(obj);		 
						count = namedParamJdbcTemplate.update(updateQry, paramSource);
						
					//////////////////////////////////INSERT NEW LEVEL /////////////////////////////////////////////////
						
					if(obj.getLevel_status().equals("Closed")) {
						if(count > 0 && !StringUtils.isEmpty(obj.getEmployee_code()) ) {
							obj.setStatus("In Progress");
							String insertQry = "INSERT INTO [safety_ims_workflow] "
												+ "(document_no,approver_type,approver_code,incident_category,status,assigned_on,level_status)"
												+ " VALUES "
												+ "(:document_code,:approver_type,:employee_code,:incident_category,:status,getdate(),:status)";
							paramSource = new BeanPropertySqlParameterSource(obj);		 
						    count = namedParamJdbcTemplate.update(insertQry, paramSource);
						}
					}	
				}else {
					String updateQry = "Update [safety_ims_workflow] set "
							+ " status= :statusChanged,action_taken= getdate(),corrective_action= :corrective_action,level_status= :level_status,"
							+ "preventive_action= :preventive_action,remarks= :remarks,attachment= :attachment "
							+ "where document_no = :document_code and approver_type= :approver_type_before and approver_code= :employee_code_before and status <> 'Send Back' and status <> 'Sent Back' ";
						BeanPropertySqlParameterSource paramSource = new BeanPropertySqlParameterSource(obj);		 
						count = namedParamJdbcTemplate.update(updateQry, paramSource);
						
						 String fileName  = "";
							/*
							 * for(int i =0; i < obj.getMediaList().length; i++) { MultipartFile
							 * multipartFile = obj.getMediaList()[i]; if (null != multipartFile &&
							 * !multipartFile.isEmpty() || !StringUtils.isEmpty(obj.getDocs()) &&
							 * obj.getDocs().length > 0) { if(null != multipartFile &&
							 * !multipartFile.isEmpty()) { String saveDirectory =
							 * CommonConstants.SAFETY_FILE_SAVING_PATH + obj.getDocument_code() +
							 * File.separator+ obj.getApprover_type_name() + File.separator; fileName =
							 * multipartFile.getOriginalFilename(); if (null != multipartFile &&
							 * !multipartFile.isEmpty()) { FileUploads.singleFileSaving(multipartFile,
							 * saveDirectory, fileName); } }else { fileName = obj.getDocs()[i]; }
							 * obj.setAttachment(fileName); } }
							 */
							
					//////////////////////////////////INSERT NEW LEVEL /////////////////////////////////////////////////
						if(count > 0 && !StringUtils.isEmpty(obj.getEmployee_code()) || !StringUtils.isEmpty(obj.getEmployee_code_before())) {
							if(count > 0 && !StringUtils.isEmpty(obj.getEmployee_code())) {
								obj.setStatus("In Progress");
								String insertQry = "INSERT INTO [safety_ims_workflow] "
													+ "(document_no,approver_type,approver_code,incident_category,status,assigned_on,level_status)"
													+ " VALUES "
													+ "(:document_code,:approver_type,:employee_code,:incident_category,:status,getdate(),:status)";
								paramSource = new BeanPropertySqlParameterSource(obj);		 
							    count = namedParamJdbcTemplate.update(insertQry, paramSource);
							}

						    
				///// ROOT CAUSE TABLE  /////////////////////////////////////////  
						    obj.setStatusChanged("Inactive");
							String inactiveQry = "Update [capa] set status= :statusChanged where document_code = :document_code";
								paramSource = new BeanPropertySqlParameterSource(obj);		 
								count = namedParamJdbcTemplate.update(inactiveQry, paramSource);
								
								String inactiveraQry = "Update [rca] set status= :statusChanged where document_code = :document_code";
								paramSource = new BeanPropertySqlParameterSource(obj);		 
								count = namedParamJdbcTemplate.update(inactiveraQry, paramSource);
								
						    obj.setStatus("Active");
						    String rc_insertQry = "INSERT INTO [rca] "
									+ "(document_code,incident_type,incident_seviourity,ua_type,uc_type,management_type,status,investigation_team)"
									+ " VALUES "
									+ "(:document_code,:incident_types,:incident_seviourity,:ua_type,:uc_type,:management_type,:status,:investigation_team)";
							paramSource = new BeanPropertySqlParameterSource(obj);		 
						    count = namedParamJdbcTemplate.update(rc_insertQry, paramSource);
			    ///// CA & PA TABLE /////////////////////////////////////////
						    fileName  = "";
						    int arraySize = 0;
						    if(!StringUtils.isEmpty(obj.getCas()) && !StringUtils.isEmpty(obj.getPas())) {
						    	if(!StringUtils.isEmpty(obj.getRemarkss()) && obj.getRemarkss().length > 0) {
									obj.setRemarkss(CommonMethods.replaceEmptyByNullInSringArray(obj.getRemarkss()));
									if(arraySize < obj.getRemarkss().length) {
										arraySize = obj.getRemarkss().length;
									}
								}
						    	if(!StringUtils.isEmpty(obj.getCas()) && obj.getCas().length > 0) {
									obj.setCas(CommonMethods.replaceEmptyByNullInSringArray(obj.getCas()));
									if(arraySize < obj.getCas().length) {
										arraySize = obj.getCas().length;
									}
								}
						    	if(!StringUtils.isEmpty(obj.getPas()) && obj.getPas().length > 0) {
									obj.setPas(CommonMethods.replaceEmptyByNullInSringArray(obj.getPas()));
									if(arraySize < obj.getPas().length) {
										arraySize = obj.getPas().length;
									}
								}
					
						    	for(int i =0; i < obj.getCas().length; i++) {
					    		if(!StringUtils.isEmpty(obj.getDocs())) {
					    			fileName = obj.getDocs()[i];
									obj.setAttachment(fileName);
					    		}
						    	MultipartFile multipartFile = obj.getMediaList()[i];
								if (null != multipartFile && !multipartFile.isEmpty() || !StringUtils.isEmpty(obj.getDocs()) && obj.getDocs().length > 0) {
									if(null != multipartFile && !multipartFile.isEmpty()) {
										String saveDirectory = CommonConstants.SAFETY_FILE_SAVING_PATH + obj.getDocument_code() + File.separator+ obj.getApprover_type_name() + File.separator;
										fileName = multipartFile.getOriginalFilename();
										if (null != multipartFile && !multipartFile.isEmpty()) {
											FileUploads.singleFileSaving(multipartFile, saveDirectory, fileName);
										}
									}
									obj.setAttachment(fileName);
								}
						    	if(!StringUtils.isEmpty(obj.getCas()) && !StringUtils.isEmpty(obj.getPas())) {
						    		obj.setCa((!StringUtils.isEmpty(obj.getCas()[i])?obj.getCas()[i]:null));
						    		obj.setPa((!StringUtils.isEmpty(obj.getPas()[i])?obj.getPas()[i]:null));
						    		obj.setRemarks((!StringUtils.isEmpty(obj.getRemarkss()[i])?obj.getRemarkss()[i]:null));
							    	 obj.setTentative_date(null);
								     obj.setTentative_date(DateParser.parse(obj.getTentative_dates()[i]));
							    	
						    		String ca_pa_insertQry = "INSERT INTO [capa] "
								    		+ "(document_code,ca,pa,person_responsible,tentative_date,remarks,attachments,status)"
											+ " VALUES "
											+ "(:document_code,:ca,:pa,:employee_code_before,:tentative_date,:remarks,:attachment,:status)";
									paramSource = new BeanPropertySqlParameterSource(obj);		 
								    count = namedParamJdbcTemplate.update(ca_pa_insertQry, paramSource);
						    	}
						    	 
						    }
						    
						}else {
							throw new ArrayIndexOutOfBoundsException("");
						}
					}
				  }
				
			}
			  if(count > 0) {
				if(!StringUtils.isEmpty(obj.getEmail_id()) ) {
					String link_url =CommonConstants.HOST+"/reirm/update-irm-form/" ;
					String subject = "Acknowledgment!";
					if(!StringUtils.isEmpty(obj.getEmployee_code()) ) {
						EMailSender emailSender = new EMailSender();
						Mail mail = new Mail();
						//mail.setMailFrom(obj.getEmail_id());
						mail.setMailTo(obj.getEmail_id());
						mail.setMailSubject("Incident - Report || Safety");
						String body = "Hi Team,"
								+ "\n\n The Incident Reviewed by "+obj.getUser_name()+"( "+obj.getUser_id()+" )"
										+ ". for more details \n\n <br> Please follow the link  <a href="+link_url+obj.getDocument_code()+"><button>Click Here</button></a>";
						emailSender.send(mail.getMailTo(), mail.getMailSubject(), body ,obj,subject);
					}else {
						EMailSender emailSender = new EMailSender();
						Mail mail = new Mail();
						//mail.setMailFrom(obj.getEmail_id());
						mail.setMailTo(obj.getEmail_id());
						mail.setMailSubject("Incident - Report || Safety");
						String body = "Hi Team,"
								+ "\n\n The Incident Has Reviewed and Closed by "+obj.getUser_name()+"( "+obj.getUser_id()+" )"
										+ ". for more details \n\n <br> Please follow the link  <a href="+link_url+obj.getDocument_code()+"><button>Click Here</button></a>";
						emailSender.send(mail.getMailTo(), mail.getMailSubject(), body ,obj,subject);
					}
					
				}
				
				if(StringUtils.isEmpty(obj.getEmployee_code()) && StringUtils.isEmpty(obj.getSb_notes()) && "Closed".equals(obj.getLevel_status()) ) {
					obj.setStatusChanged("Resolved");
					String finalStatusUpdateQry = "Update [safety_ims] set "
							+ " status= :statusChanged,modified_by= :user_id,mdified_date= getdate() where document_code = :document_code";
					BeanPropertySqlParameterSource paramSource = new BeanPropertySqlParameterSource(obj);		 
						count = namedParamJdbcTemplate.update(finalStatusUpdateQry, paramSource);
						   ///////////// REWARDS //////////////////////////////////
					    
					    String rewardQry = "update user_profile set reward_points = COALESCE(reward_points, 0) + 20 "
								+ " where user_id= :created_by";
							 paramSource = new BeanPropertySqlParameterSource(obj);		 
						    count = namedParamJdbcTemplate.update(rewardQry, paramSource);	
				}
				flag = true;
			}
			transactionManager.commit(status);
		}catch (Exception e) {
			transactionManager.rollback(status);
			e.printStackTrace();
			throw new Exception(e);
		}
		return flag;
	}

	public List<IRM> getIRMHistoryList(IRM obj) throws Exception {
		List<IRM> objsList = new ArrayList<IRM>();
		List<IRM> objsList1 = new ArrayList<IRM>();
		List<IRM> merge = new ArrayList<IRM>();
		try {
			String qry = "SELECT document_no"
					+ "      ,approver_type"
					+ "      ,approver_code,ss.created_by ,u1.user_name "
					+ "      ,s.status"
					+ "      ,FORMAT(assigned_on, 'dd-MMM-yy  HH:mm') as assigned_on"
					+ "      ,FORMAT(action_taken, 'dd-MMM-yy  HH:mm') as action_taken"
					+ "      ,corrective_action"
					+ "      ,preventive_action"
					+ "      ,remarks,u.user_name,sb_notes,FORMAT(sb_date, 'dd-MMM-yy  HH:mm') as sb_date"
					+ "      ,attachment FROM [safety_ims_workflow] s "
					+ "left join [safety_ims] ss on s.document_no = ss.document_code "
					+ " left join [user_profile] u on s.approver_code = u.user_id "
					+ " inner join [user_profile] u1 on ss.created_by = u1.user_id "
					+ " where  s.document_no is not null and  s.document_no <> ''  "; 
			int arrSize = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getDocument_code())) {
				qry = qry + " and s.document_no = ? ";
				arrSize++;
			}
			qry = qry + " order by   s.id desc";
			Object[] pValues = new Object[arrSize];
			int i = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getDocument_code())) {
				pValues[i++] = obj.getDocument_code();
			}
			objsList =  jdbcTemplate.query( qry, pValues, new BeanPropertyRowMapper<IRM>(IRM.class));
			
			String qry2 = "select document_code as document_no,u.base_role as approver_type, s.created_by as approver_code "
					+ " ,FORMAT(s.created_date, 'dd-MMM-yy  HH:mm') as assigned_on,u.user_name from safety_ims s "
					+ "left join [user_profile] u on s.created_by = u.user_id where document_code is not null ";
			int arrSize1 = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getDocument_code())) {
				qry2 = qry2 + " and s.document_code = ? ";
				arrSize1++;
			}
			qry2 = qry2 + " order by   s.id desc";
			Object[] pValues1 = new Object[arrSize1];
			int j = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getDocument_code())) {
				pValues1[j++] = obj.getDocument_code();
			}
			objsList1 =  jdbcTemplate.query( qry2, pValues1, new BeanPropertyRowMapper<IRM>(IRM.class));
			
			merge.addAll(objsList);
		    merge.addAll(objsList1);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		return merge;
	}

	public List<RoleMapping> getRoleMappedOrNot(RoleMapping obj) throws Exception {
		List<RoleMapping> objsList = new ArrayList<RoleMapping>();
		try {
			String qry = "SELECT role_code ,user_name,email_id from role_mapping s left join user_profile up on s.employee_code = up.user_id where role_code is not null   "; 
			int arrSize = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				qry = qry + " and s.project = ? ";
				arrSize++;
			}
			/*
			 * if(!StringUtils.isEmpty(obj) &&
			 * !StringUtils.isEmpty(obj.getDepartment_code())) { qry = qry +
			 * " and s.department_code = ? "; arrSize++; }
			 */
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSafety_type())) {
				qry = qry + " and s.safety_type = ? ";
				arrSize++;
			}
			Object[] pValues = new Object[arrSize];
			int i = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				pValues[i++] = obj.getProject_code();
			}
			/*
			 * if(!StringUtils.isEmpty(obj) &&
			 * !StringUtils.isEmpty(obj.getDepartment_code())) { pValues[i++] =
			 * obj.getDepartment_code(); }
			 */
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSafety_type())) {
				pValues[i++] = obj.getSafety_type();
			}
			objsList =  jdbcTemplate.query( qry, pValues, new BeanPropertyRowMapper<RoleMapping>(RoleMapping.class));
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		return objsList;
	}

	public List<IRM> getUserListIRMUpdate(IRM irm) throws SQLException {
		List<IRM> menuList = null;
		try{  
			String qry = "select user_id,user_name,base_department,base_role from [user_profile] ";
			menuList = jdbcTemplate.query( qry, new BeanPropertyRowMapper<IRM>(IRM.class));
			
		}catch(Exception e){ 
			e.printStackTrace();
			throw new SQLException(e.getMessage());
		}
		return menuList;
	}

	public List<IRM> getIRMListAlert() throws Exception {
		List<IRM> alert_levels = null; IRM obj = new IRM();
		try {
			String statusList [] = {"'In Progress'","'In Progress'","'In Progress'","'Resolved'","Null","Total"};  
			String roleList [] = {"'IRL1'","'IRL2'","'IRL3'","'IRL3'","Null","Total"};  
			String statusPList [] = {"In Progress","Action Taken <br> Review In Progress"," Review Done <br> Closure Pending","Resolved","No Reviewer Found","Total Count"};  
			
			Map<String,List<IRM>> alerts = new LinkedHashMap<String, List<IRM>>();
			alerts = new LinkedHashMap<String, List<IRM>>();
			for (int i =0; i<statusList.length; i++) {
				String subQry = " n.status = "+statusList[i]+" and  approver_type = "+roleList[i]+" and ";
				String subQry2 = " n.status = "+statusList[i]+" and ";

				if(("'Resolved'".contentEquals(statusList[i]))) {
					 subQry = " c.status = "+statusList[i]+" and  approver_type = "+roleList[i]+" and ";
					 subQry2 = " c.status = "+statusList[i]+"  and ";

				}else if( "Null".contentEquals(statusList[i])) {
					 subQry = " c.status is null and  approver_type is null and ";
					 subQry2 = " c.status is null and  ";

				}
				if(("Total".contentEquals(roleList[i]))) {
					 subQry = "  ";

				}
				String qry =" SELECT top (1) "
						+ "(select count([document_no]) from safety_ims_workflow n left join safety_ims c on c.document_code = n.document_no "
						+ "where  "+subQry+" c.[incident_type] = 'AC'  and created_date >= DATEADD(DAY, -1, GETDATE()) AND created_date <  GETDATE()) as AC  "
						+ ",(select count([document_no]) from safety_ims_workflow n left join safety_ims c on c.document_code = n.document_no "
						+ "where  "+subQry+" c.[incident_type] = 'NM'  and created_date >= DATEADD(DAY, -1, GETDATE()) AND created_date <  GETDATE()) as NM  "
				 		+ ",(select count([document_no]) from safety_ims_workflow n left join safety_ims c on c.document_code = n.document_no "
						+ "where  "+subQry+" c.[incident_type] = 'UA'  and created_date >= DATEADD(DAY, -1, GETDATE()) AND created_date <  GETDATE())  as UA  "
						+ ",(select count([document_no]) from safety_ims_workflow n left join safety_ims c on c.document_code = n.document_no "
						+ "where  "+subQry+" c.[incident_type] = 'UC'  and created_date >= DATEADD(DAY, -1, GETDATE()) AND created_date <  GETDATE())  as UC  "
						+ "  FROM [safetyDB].[dbo].[safety_ims_workflow] n "
						+ "  left join safety_ims c on c.document_code = n.document_no "
						+ "  left join [role_master] r on n.approver_type = r.[incident_report] or n.approver_type <> r.[incident_report] "
						+ "  group by c.[incident_type]";
				alert_levels = jdbcTemplate.query( qry, new BeanPropertyRowMapper<IRM>(IRM.class));
				if(!StringUtils.isEmpty(alert_levels) && alert_levels.size() > 0) {
					alerts.put(statusPList[i], alert_levels);
				}
			}
			
			EMailSender emailSender = new EMailSender();
			Set<String> nameSet = new HashSet<>();
			if(alerts != null && alerts.size() > 0) {
				SimpleDateFormat monthFormat = new SimpleDateFormat("dd-MMM-YYYY");
	            String today_date = monthFormat.format(new Date()).toUpperCase();
	          
	            monthFormat = new SimpleDateFormat("dd-MMM-YYYY");
	            String yesterday_date = monthFormat.format(yesterday()).toUpperCase();
	            System.out.println(yesterday_date);
	            SimpleDateFormat yearFormat = new SimpleDateFormat("YYYY");
	            String current_year = yearFormat.format(new Date()).toUpperCase();
	            
				String emailSubject = "Daily Safety Report | AAYUSH";
				String emailSubjectName = "Aayush - Safety First";
				Mail mail = new Mail();
				mail.setMailTo("amarnathreddy@resustainability.com,suryaprakash.a@resustainability.com,muralimohan.h@resustainability.com"); 
				mail.setMailBcc("businessapps.appworks@resustainability.com");
				mail.setMailSubject(emailSubject);
				mail.setTemplateName("SafetyDaily.vm");
				
				emailSender.sendIRMEmailAlerts(mail,alerts,today_date,yesterday_date,current_year,emailSubjectName); 
			}
				
			Thread.sleep(1000*10);//time is in ms (1000 ms = 1 second)
			
			
			
			
		}catch(Exception e){ 
			e.printStackTrace();
			throw new Exception(e);
		}
		return alert_levels;
	}

	private static Date yesterday() {
	    final Calendar cal = Calendar.getInstance();
	    cal.add(Calendar.DATE, -1);
	    return cal.getTime();
	}
	private static Date lastMonth() {
	    final Calendar cal = Calendar.getInstance();
	    cal.add(Calendar.DATE, -30);
	    return cal.getTime();
	}

	public List<IRM> getIRMListAlertMonthly() throws Exception {
		List<IRM> alert_levels = null; IRM obj = new IRM();
		try {
			String statusList [] = {"'In Progress'","'In Progress'","'In Progress'","'Resolved'","Null","Total"};  
			String roleList [] = {"'IRL1'","'IRL2'","'IRL3'","'IRL3'","Null","Total"};  
			String statusPList [] = {"In Progress","Action Taken <br> Review In Progress"," Review Done <br> Closure Pending","Resolved","No Reviewer Found","Total Count"};  
			
			Map<String,List<IRM>> alerts = new LinkedHashMap<String, List<IRM>>();
			alerts = new LinkedHashMap<String, List<IRM>>();
			for (int i =0; i<statusList.length; i++) {
				String subQry = " n.status = "+statusList[i]+" and  approver_type = "+roleList[i]+" and ";
				String subQry2 = " n.status = "+statusList[i]+" and ";

				if(("'Resolved'".contentEquals(statusList[i]))) {
					 subQry = " c.status = "+statusList[i]+" and  approver_type = "+roleList[i]+" and ";
					 subQry2 = " c.status = "+statusList[i]+"  and ";

				}else if( "Null".contentEquals(statusList[i])) {
					 subQry = " c.status is null and  approver_type is null and ";
					 subQry2 = " c.status is null and  ";

				}
				String qry =" SELECT top (1) "
						+ "(select count([document_no]) from safety_ims_workflow n left join safety_ims c on c.document_code = n.document_no "
						+ "where  "+subQry+" c.[incident_type] = 'AC'  and DATEDIFF(day,created_date,GETDATE()) between  0 and 30) as AC  "
						+ ",(select count([document_no]) from safety_ims_workflow n left join safety_ims c on c.document_code = n.document_no "
						+ "where  "+subQry+" c.[incident_type] = 'NM'  and DATEDIFF(day,created_date,GETDATE()) between  0 and 30) as NM  "
				 		+ ",(select count([document_no]) from safety_ims_workflow n left join safety_ims c on c.document_code = n.document_no "
						+ "where  "+subQry+" c.[incident_type] = 'UA'  and DATEDIFF(day,created_date,GETDATE()) between  0 and 30)  as UA  "
						+ ",(select count([document_no]) from safety_ims_workflow n left join safety_ims c on c.document_code = n.document_no "
						+ "where  "+subQry+" c.[incident_type] = 'UC'  and DATEDIFF(day,created_date,GETDATE()) between  0 and 30)  as UC  "
						+ "  FROM [safetyDB].[dbo].[safety_ims_workflow] n "
						+ "  left join safety_ims c on c.document_code = n.document_no "
						+ "  left join [role_master] r on n.approver_type = r.[incident_report] or n.approver_type <> r.[incident_report] "
						+ "  group by c.[incident_type]";
				if(("Total".contentEquals(roleList[i]))) {
					 subQry = "  ";
					 qry =" SELECT top (1) "
								+ "(select count([document_code]) from  safety_ims c "
								+ "where  "+subQry+" c.[incident_type] = 'AC'  and DATEDIFF(day,created_date,GETDATE()) between  0 and 30) as AC  "
								+ ",(select count([document_code]) from safety_ims c  "
								+ "where  "+subQry+" c.[incident_type] = 'NM'  and DATEDIFF(day,created_date,GETDATE()) between  0 and 30) as NM  "
						 		+ ",(select count([document_code]) from  safety_ims c  "
								+ "where  "+subQry+" c.[incident_type] = 'UA'  and DATEDIFF(day,created_date,GETDATE()) between  0 and 30)  as UA  "
								+ ",(select count([document_code]) from safety_ims c  "
								+ "where  "+subQry+" c.[incident_type] = 'UC'  and DATEDIFF(day,created_date,GETDATE()) between  0 and 30)  as UC  "
								+ "  FROM [safetyDB].[dbo].[safety_ims_workflow] n "
								+ "  left join safety_ims c on c.document_code = n.document_no "
								+ "  left join [role_master] r on n.approver_type = r.[incident_report] or n.approver_type <> r.[incident_report] "
								+ "  group by c.[incident_type]";
				}
				alert_levels = jdbcTemplate.query( qry, new BeanPropertyRowMapper<IRM>(IRM.class));
				if(!StringUtils.isEmpty(alert_levels) && alert_levels.size() > 0) {
					alerts.put(statusPList[i], alert_levels);
				}
			}
			
			EMailSender emailSender = new EMailSender();
			Set<String> nameSet = new HashSet<>();
			if(alerts != null && alerts.size() > 0) {
				SimpleDateFormat monthFormat = new SimpleDateFormat("dd-MMM-YYYY");
	            String today_date = monthFormat.format(new Date()).toUpperCase();
	          
	            monthFormat = new SimpleDateFormat("dd-MMM-YYYY");
	            String yesterday_date = monthFormat.format(lastMonth()).toUpperCase();
	            System.out.println(yesterday_date);
	            SimpleDateFormat yearFormat = new SimpleDateFormat("YYYY");
	            String current_year = yearFormat.format(new Date()).toUpperCase();
	            
				String emailSubject = "Monthly Safety Report | AAYUSH";
				String emailSubjectName = "Aayush - Safety First";
				Mail mail = new Mail();
				mail.setMailTo("amarnathreddy@resustainability.com,suryaprakash.a@resustainability.com,muralimohan.h@resustainability.com"); 
				mail.setMailBcc("businessapps.appworks@resustainability.com");
				mail.setMailSubject(emailSubject);
				mail.setTemplateName("MonthlySafertAlerts.vm");
				
				emailSender.sendIRMEmailAlerts(mail,alerts,today_date,yesterday_date,current_year,emailSubjectName); 
			}
				
			Thread.sleep(1000*10);//time is in ms (1000 ms = 1 second)
			
			
			
			
		}catch(Exception e){ 
			e.printStackTrace();
			throw new Exception(e);
		}
		return alert_levels;
	}

	public List<IRM> UserActivityCheck() throws Exception{
		List<IRM> userList = null; 
		List<IRM> dicList = null; 
		try {
			String qry =" SELECT u.user_id,(select user_Name from user_profile where user_id = u.user_id ) as user_name, "
					+ "(select email_id from user_profile where user_id = u.user_id ) as email_id, "
					+ "(select count([document_no]) from safety_ims_workflow where  approver_code = u.user_id and [action_taken] is null and status <> 'Reviewed') as incidents_count, "
					+ "DATEDIFF(day,MAX([assigned_on]),GETDATE() )  AS 'daysNotLogined' "
					+ "FROM [user_audit_log] u "
					+ "	left join safety_ims_workflow rm on rm.approver_code = u.user_id  "
					+ "    left join user_profile up on rm.approver_code = u.user_id "
					+ "    where rm.status <> 'Reviewed' and rm.status not like '%back%' and [action_taken] is null  "
					+ "	and  [assigned_on] < DateAdd(day,-15,GETDATE() )  "
					+ "    group by u.user_id order by MAX([user_login_time]) desc";
			
			userList = jdbcTemplate.query( qry, new BeanPropertyRowMapper<IRM>(IRM.class));
			if(userList.size() > 0) {
				for(IRM obj : userList) {
					String qry2 =" SELECT STRING_AGG([document_no],', ')  as [document_code] "
							+ "  FROM [safety_ims_workflow] where approver_code is not null   ";
					int arrSize = 0;
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getUser_id())) {
						qry2 = qry2 + " and approver_code = ? ";
						arrSize++;
					}
					qry2 = qry2 + " and status <> 'Reviewed' and status not like '%back%'";
					Object[] pValues = new Object[arrSize];
					int i = 0;
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getUser_id())) {
						pValues[i++] = obj.getUser_id();
					}
					
					dicList = jdbcTemplate.query( qry2,pValues, new BeanPropertyRowMapper<IRM>(IRM.class));
					
					EMailSender emailSender = new EMailSender();
					String link_url =CommonConstants.HOME;
					Mail mail = new Mail();
					//mail.setMailTo(obj.getEmail_id());
					mail.setMailTo("saidileep.p@resustainability.com");
					mail.setMailSubject("🔔 Action Remainder | AAYUSH | Re Sustainability");
					String body = "Hi, "+obj.getUser_name()+"<br>"
							+ " Greetings From <b>Re Sustainability | AAYUSH</b>"
							+ "<br> We Noticed, There is no Login Activity Form you since <b>"+obj.getDaysNotLogined()+" Days </b> "
							+ "<br> There are <b>"+obj.getIncidents_count()+"</b> Incidents (<b>"+dicList.get(0).getDocument_code()+"</b>) awaiting your Evaluation. <br>"
							+ " Please Login <a href="+link_url+"><button>Click Here</button></a> and Complete your Assignments ASAP. "
							+ "<br><br><br>"
							+ "Thanks"
							+ "<p style='color : red'><b>AAYUSH | Safety First</b></p>"
							+ "<b>Re Sustainability</b>";
					String subject = "Remainder Alerts";
					emailSender.send(mail.getMailTo(), mail.getMailSubject(), body,obj,subject);
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		
		return userList;
	}

	public boolean irmUpdateFilesSubmit(IRM obj) throws Exception {
		int count = 0;
		boolean flag = false;
		TransactionDefinition def = new DefaultTransactionDefinition();
		TransactionStatus status = transactionManager.getTransaction(def);
		try {
			String file_name = "";
			String photo = "";
			if(!StringUtils.isEmpty(obj.getPhoto())) {
				photo = obj.getPhoto();
			}
			if(!StringUtils.isEmpty(obj.getMediaList())) {
			for (int i = 0; i < (obj.getMediaList().length); i++) {
				MultipartFile multipartFile = obj.getMediaList()[i];
				if (null != multipartFile && !multipartFile.isEmpty()) {
					String saveDirectory = CommonConstants.SAFETY_FILE_SAVING_PATH + obj.getDocument_code() + File.separator;
					String fileName = multipartFile.getOriginalFilename();
					//obj.setCreated_date(DateParser.parse(date));
					if (null != multipartFile && !multipartFile.isEmpty()) {
						FileUploads.singleFileSaving(multipartFile, saveDirectory, fileName);
					}
					file_name = file_name+ fileName+",";
				}
			}
			}
			if(!StringUtils.isEmpty(obj.getImage_list())) {
				for (int i = 0; i < (obj.getImage_list().length); i++) {
					
					    byte[] decodedBytes = Base64.getDecoder().decode(obj.getImage_list()[i]);
				        byte[] pdfBytes = decodedBytes; 
				        String saveDirectory = CommonConstants.SAFETY_FILE_SAVING_PATH + obj.getDocument_code() + File.separator + File.separator;
				        File directory = new File(saveDirectory);
		                if (!directory.exists()) {
		                	directory.mkdirs();
		                	boolean flag1 = OSValidator.isUnix();
		                	if(flag1) {
		                    	String perm = "rwxrwxrwx";
		    	            	Set<PosixFilePermission> permissions = PosixFilePermissions.fromString(perm);
		    	            	Files.setPosixFilePermissions(directory.toPath(), permissions);
		                	}
		                }
				        Files.write(Paths.get( saveDirectory+obj.getFilenameAndExtList()[i]), pdfBytes);
				        file_name = file_name+ obj.getFilenameAndExtList()[i]+","+photo;
				}
			}
			if(!StringUtils.isEmpty(file_name)) {
				StringBuilder builder = new StringBuilder(file_name);
				int lastindex = file_name.lastIndexOf(",");
				builder.replace(lastindex, lastindex + 1, "" );
				file_name = builder.toString();
				obj.setPhoto(file_name+","+photo);
			}
			NamedParameterJdbcTemplate namedParamJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
			String insertQry = "UPDATE [safety_ims] set photo= :photo where document_code= :document_code_files ";
			BeanPropertySqlParameterSource paramSource = new BeanPropertySqlParameterSource(obj);		 
		    count = namedParamJdbcTemplate.update(insertQry, paramSource);
			if(count > 0) {
				flag = true;
		 }
			transactionManager.commit(status);
		}catch (Exception e) {
			transactionManager.rollback(status);
			e.printStackTrace();
			throw new Exception(e);
		}
		return flag;
	}

	public int getTotalRecords(IRM obj, String searchParameter) throws Exception {
		int totalRecords = 0;
		try {
			int arrSize = 0;
			String qry = "select count( document_code) as total_records  from [safety_ims] c "
				+ " left join [safety_ims_workflow] up on c.document_code = up.document_no "
				+ " left join [project] p on c.project_code = p.project_code "
				+ " left join [sbu] sb on p.sbu_code = sb.sbu_code"
				+ " left join [department] d on c.department_code = d.department_code "
				+ " left join [project_location] l on c.location = l.location_code "
				+ " left join [incident] rm on c.incident_type = rm.incident_code "
				+ " left join [user_profile] u on c.created_by = u.user_id "
				+ " left join [user_profile] u1 on up.approver_code = u1.user_id "
				+ " where  document_code is not null   ";
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
			qry = qry + " and p.sbu_code = ? ";
			arrSize++;
		}
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
			qry = qry + " and c.project_code = ? ";
			arrSize++;
		}
								
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
			qry = qry + " and c.incident_type = ?";
			arrSize++;
		}	
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
			qry = qry + " and  c.status = ? ";
			arrSize++;
		}
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
			qry = qry + " and CONVERT(date, c.created_date)  BETWEEN ? and ?  ";
			arrSize++;
			arrSize++;
		}
		else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
			qry = qry + " and  CONVERT(date, c.created_date) = ? ";
			arrSize++;
		}
		else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
			qry = qry + " and  CONVERT(date, c.created_date) = ? ";
			arrSize++;
		}
		if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
			qry = qry + " and ( c.created_by = ? or approver_code in(select distinct approver_code from [safety_ims_workflow] where approver_code = ? ))";
			arrSize++;
			arrSize++;
		}
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			qry = qry + " and c.created_by = ?";
			arrSize++;
		}
		if(!StringUtils.isEmpty(obj) && StringUtils.isEmpty(obj.getI_pending())) {
			//qry = qry + " and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30 ";
		}
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
			qry = qry + " and  up.status = ? ";
			arrSize++;
		}
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed()) ) {
			qry = qry + " and  c.status = ? and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
			arrSize++;
		}
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
			qry = qry + " and  c.status = ? and (up.approver_code = ? or  c.created_by = ?)  and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
			arrSize++;
			arrSize++;
			arrSize++;
		}
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_no_reviewer())) {
			qry = qry + " and  c.status is null ";
		}
			
		if(StringUtils.isEmpty(obj.getPageIndex())) {
			obj.setPageIndex("0");
		}
		if(!StringUtils.isEmpty(searchParameter)) {
			qry = qry + " and (c.document_code like ? or c.project_code like ? or p.project_name like ?"
					+ " or u1.user_name like ? or up.approver_code like ? or c.document_code like ? or d.department_name like ? "
					+ "or c.risk_type like ? or c.created_by like ? or u.user_name like ? or c.status like ? or approver_type like ? )";
			arrSize++;
			arrSize++;
			arrSize++;
			arrSize++;
			arrSize++;
			arrSize++;
			arrSize++;
			arrSize++;
			arrSize++;
			arrSize++;
			arrSize++;
			arrSize++;
			
		}	
		
		Object[] pValues = new Object[arrSize];
		int i = 0;
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
			pValues[i++] = obj.getFrom_date();
			pValues[i++] = obj.getTo_date();
		}
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
			pValues[i++] = obj.getSbu_code();
		}
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
			pValues[i++] = obj.getProject_code();
		}
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
			pValues[i++] = obj.getStatus();
		}
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
			pValues[i++] = obj.getIncident_type();
		}
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
			pValues[i++] = obj.getFrom_date();
			pValues[i++] = obj.getTo_date();
		}
		else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
			pValues[i++] = obj.getFrom_date();
		}
		else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
			pValues[i++] = obj.getTo_date();
		}
		if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
			pValues[i++] = obj.getUser(); pValues[i++] = obj.getUser();
		}
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
		pValues[i++] = obj.getAdmin_incidents();
		}
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
			pValues[i++] = obj.getI_pending();
		}
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
			pValues[i++] = obj.getI_completed();
		}
		if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
			pValues[i++] = obj.getI_completed();
			pValues[i++] = obj.getUser();
			pValues[i++] = obj.getUser();
		}
		
		if(!StringUtils.isEmpty(searchParameter)) {
			pValues[i++] = "%"+searchParameter+"%";
			pValues[i++] = "%"+searchParameter+"%";
			pValues[i++] = "%"+searchParameter+"%";
			pValues[i++] = "%"+searchParameter+"%";
			pValues[i++] = "%"+searchParameter+"%";
			pValues[i++] = "%"+searchParameter+"%";
			pValues[i++] = "%"+searchParameter+"%";
			pValues[i++] = "%"+searchParameter+"%";
			pValues[i++] = "%"+searchParameter+"%";
			pValues[i++] = "%"+searchParameter+"%";
			pValues[i++] = "%"+searchParameter+"%";
			pValues[i++] = "%"+searchParameter+"%";
			
		}
		
			totalRecords = jdbcTemplate.queryForObject( qry,pValues,Integer.class);
		}catch(Exception e){ 
			e.printStackTrace();
			throw new Exception(e);
		}
		return totalRecords;
	}

	public List<IRM> getIRMLAzyList(IRM obj, int startIndex,  int offset, String searchParameter) throws Exception {
		List<IRM> objsList = null;
		try {
			int arrSize = 0;
			String qry =" select c.id,c.document_code,(select count(*) from [safety_ims] where status is null) as noCounts,(select count(*) from [safety_ims] where status = 'In Progress' ";
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
						qry = qry + " and CONVERT(date, created_date)  BETWEEN ? and ?  ";
						arrSize++;
						arrSize++;
					}else {
						//qry = qry + "and DATEDIFF(day,created_date,GETDATE()) between  0 and 30 ";
					}
					qry = qry + ") as counts,FORMAT(action_taken, 'dd-MMM-yy  HH:mm') as action_taken,incident_category,"
					+ "(select max(role_code) from role_mapping where project = c.project_code and [department_code] = d.[department_code] and [safety_type] = c.[incident_type] ) as maxRole,"
					+ "(select max(approver_type) from safety_ims_workflow where status = 'In Progress' and safety_ims_workflow.document_no = c.document_code) as maxRole2,";
					qry = qry +" (select count( distinct c.document_code) from safety_ims c left join [safety_ims_workflow] up on c.document_code = up.document_no  left join [project] p on c.project_code = p.project_code left join [sbu] sb on p.sbu_code = sb.sbu_code where c.incident_type is not null   ";
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
						qry = qry + " and p.sbu_code = ? ";
						arrSize++;
					}
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
						qry = qry + " and c.project_code = ? ";
						arrSize++;
					}
					
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
						qry = qry + " and  c.status = ? ";
						arrSize++;
					}
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
						qry = qry + " and  c.incident_type = ? ";
						arrSize++;
					}
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
						qry = qry + " and CONVERT(date, c.created_date)  BETWEEN ? and ?  ";
						arrSize++;
						arrSize++;
					}
					else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
						qry = qry + " and  CONVERT(date, c.created_date) = ? ";
						arrSize++;
					}
					else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
						qry = qry + " and  CONVERT(date, c.created_date) = ? ";
						arrSize++;
					}
					if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
						qry = qry + " and ( c.created_by = ? or approver_code in(select distinct approver_code from [safety_ims_workflow] where approver_code = ? ))";
						arrSize++;	arrSize++;
					}
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
						qry = qry + " and c.created_by = ?";
						arrSize++;	
					}
					if(!StringUtils.isEmpty(obj) && StringUtils.isEmpty(obj.getFrom_date())) {
						//qry = qry + " and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30 ";
					}
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
						qry = qry + " and  c.status = ?  ";
						arrSize++;
					}

					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed()) ) {
						qry = qry + " and  c.status = ? and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
						arrSize++;
					}
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
						qry = qry + " and  c.status = ? and  (up.approver_code = ? or  c.created_by = ?) and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
						arrSize++;
						arrSize++;arrSize++;
					}
					if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_no_reviewer())) {
						qry = qry + " and  c.status is null ";
					}
					qry = qry +  " ) as all_irm ,"
							+ "  (select count(*) from [safety_ims] c "
							+ " left join [project] p on c.project_code = p.project_code "
							+ "left join [sbu] sb on p.sbu_code = sb.sbu_code where c.[status] is null";
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
								qry = qry + " and p.sbu_code = ? ";
								arrSize++;
							}
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
								qry = qry + " and c.project_code = ? ";
								arrSize++;
							}
			
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
								qry = qry + "  and c.status = ? ";
								arrSize++;
							}
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
								qry = qry + " and c.incident_type = ? ";
								arrSize++;
							}
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
								qry = qry + " and CONVERT(date, c.created_date)  BETWEEN ? and ?  ";
								arrSize++;
								arrSize++;
							}
							else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
								qry = qry + " and  CONVERT(date, c.created_date) = ? ";
								arrSize++;
							}
							else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
								qry = qry + " and  CONVERT(date, c.created_date) = ? ";
								arrSize++;
							}
							if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
								qry = qry + " and ( c.created_by = ? or approver_code in(select distinct approver_code from [safety_ims_workflow] where approver_code = ? ))";

								arrSize++;	arrSize++;
							}
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
								qry = qry + " and c.created_by = ? ";
								arrSize++;	
							}
							if(!StringUtils.isEmpty(obj) && StringUtils.isEmpty(obj.getI_pending())) {
								//qry = qry + " and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30 ";
							}
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
								qry = qry + " and  c.status = ? ";
								arrSize++;
							}
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
								qry = qry + " and  c.status = ? and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
								arrSize++;
							}

							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
								qry = qry + " and  c.status = ? and  (up.approver_code = ? or  c.created_by = ?) and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
								arrSize++;
								arrSize++;arrSize++;
							}
							if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_no_reviewer())) {
								qry = qry + " and  c.status is null ";
							}
								
							qry = qry + " ) as not_assigned,";
							qry = qry +	"(select count( c.[document_code]) from [safety_ims] c left join [project] p on c.project_code = p.project_code left join [sbu] sb on p.sbu_code = sb.sbu_code where c.[document_code] is not null and c.status ='Resolved'  ";
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
										qry = qry + " and p.sbu_code = ? ";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
										qry = qry + " and c.project_code = ? ";
										arrSize++;
									}
					
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
										qry = qry + "  and c.status = ? ";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
										qry = qry + " and c.incident_type = ? ";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
										qry = qry + " and CONVERT(date, c.created_date)  BETWEEN ? and ?  ";
										arrSize++;
										arrSize++;
									}
									else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
										qry = qry + " and  CONVERT(date, c.created_date) = ? ";
										arrSize++;
									}
									else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
										qry = qry + " and  CONVERT(date, c.created_date) = ? ";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
										qry = qry + " and ( c.created_by = ? or approver_code in(select distinct approver_code from [safety_ims_workflow] where approver_code = ? ))";
										arrSize++;
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
										qry = qry + " and c.created_by = ?";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && StringUtils.isEmpty(obj.getI_pending())) {
										//qry = qry + " and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30 ";
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
										qry = qry + " and  c.status = ? ";
										arrSize++;
									}

									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed()) ) {
										qry = qry + " and  c.status = ? and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
										qry = qry + " and  c.status = ? and  (up.approver_code = ? or  c.created_by = ?) and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
										arrSize++;
										arrSize++;arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_no_reviewer())) {
										qry = qry + " and  c.status is null ";
									}
										
									qry = qry + " ) as active_irm,"
									+ "(select count( c.[document_code]) from [safety_ims] c left join [project] p on c.project_code = p.project_code left join [sbu] sb on p.sbu_code = sb.sbu_code where c.[document_code] is not null and c.status ='In Progress' ";
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
										qry = qry + " and p.sbu_code = ? ";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
										qry = qry + " and c.project_code = ? ";
										arrSize++;
									}
									
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
										qry = qry + " and  c.status = ? ";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
										qry = qry + " and  c.incident_type = ? ";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
										qry = qry + " and CONVERT(date, c.created_date)  BETWEEN ? and ?  ";
										arrSize++;
										arrSize++;
									}
									else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
										qry = qry + " and  CONVERT(date, c.created_date) = ? ";
										arrSize++;
									}
									else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
										qry = qry + " and  CONVERT(date, c.created_date) = ? ";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
										qry = qry + " and ( c.created_by = ? or approver_code in(select distinct approver_code from [safety_ims_workflow] where approver_code = ? ))";
										arrSize++;
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
										qry = qry + " and c.created_by = ?";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && StringUtils.isEmpty(obj.getI_pending())) {
										qry = qry + " ";
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
										qry = qry + " and  c.status = ? ";
										arrSize++;
									}
				
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed()) ) {
										qry = qry + " and  c.status = ? and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
										arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
										qry = qry + " and  c.status = ? and  (up.approver_code = ? or  c.created_by = ?) and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
										arrSize++;
										arrSize++;arrSize++;
									}
									if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_no_reviewer())) {
										qry = qry + " and  c.status is null ";
									}
									
										
									qry = qry + " ) as inActive_irm,"
					+ "rm.incident_code,rm.incident_type,approver_type,p.sbu_code,sb.sbu_name,	 c.project_code,p.project_name,c.risk_type,c.description,c.status,c.ptw_code,c.department_code,d.department_name,"
					+ "	l.location_code,l.location_name,c.created_by,u.email_id,u.user_name, COALESCE(up.approver_code, 'No Reviewer Assigneds') AS approver_code,u1.user_name as approver_name,FORMAT(c.created_date, 'dd-MMM-yy  HH:mm') as created_date from [safety_ims] c "
					+ " left join [safety_ims_workflow] up on c.document_code = up.document_no "
					+ " left join [project] p on c.project_code = p.project_code "
					+ " left join [sbu] sb on p.sbu_code = sb.sbu_code"
					+ " left join [department] d on c.department_code = d.department_code "
					+ " left join [project_location] l on c.location = l.location_code "
					+ " left join [incident] rm on c.incident_type = rm.incident_code "
					+ " left join [user_profile] u on c.created_by = u.user_id "
					+ " left join [user_profile] u1 on up.approver_code = u1.user_id "
					+ " where  document_code is not null   ";
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				qry = qry + " and p.sbu_code = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				qry = qry + " and c.project_code = ? ";
				arrSize++;
			}
									
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				qry = qry + " and c.incident_type = ?";
				arrSize++;
			}	
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				qry = qry + " and  c.status = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
				qry = qry + " and CONVERT(date, c.created_date)  BETWEEN ? and ?  ";
				arrSize++;
				arrSize++;
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
				qry = qry + " and  CONVERT(date, c.created_date) = ? ";
				arrSize++;
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
				qry = qry + " and  CONVERT(date, c.created_date) = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				qry = qry + " and ( c.created_by = ? or approver_code in(select distinct approver_code from [safety_ims_workflow] where approver_code = ? ))";
				arrSize++;
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
				qry = qry + " and c.created_by = ?";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && StringUtils.isEmpty(obj.getI_pending())) {
				//qry = qry + " and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30 ";
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				qry = qry + " and  up.status = ? ";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed()) ) {
				qry = qry + " and  c.status = ? and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				qry = qry + " and  c.status = ? and (up.approver_code = ? or  c.created_by = ?)  and DATEDIFF(day,c.created_date,GETDATE()) between  0 and 30";
				arrSize++;
				arrSize++;
				arrSize++;
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_no_reviewer())) {
				qry = qry + " and  c.status is null ";
			}
				
			if(StringUtils.isEmpty(obj.getPageIndex())) {
				obj.setPageIndex("0");
			}
			if(!StringUtils.isEmpty(searchParameter)) {
				qry = qry + " and (c.document_code like ? or c.project_code like ? or p.project_name like ?"
						+ " or u1.user_name like ? or up.approver_code like ? or c.document_code like ? or d.department_name like ? "
						+ "or c.risk_type like ? or c.created_by like ? or u.user_name like ? or c.status like ? or approver_type like ? )";
				arrSize++;
				arrSize++;
				arrSize++;
				arrSize++;
				arrSize++;
				arrSize++;
				arrSize++;
				arrSize++;
				arrSize++;
				arrSize++;
				arrSize++;
				arrSize++;
				
			}	
			if(!StringUtils.isEmpty(startIndex) && !StringUtils.isEmpty(offset)) {
				qry = qry + " order by FORMAT(c.created_date, 'dd-MMM-yy'),FORMAT(c.created_date, 'HH:mm'),approver_type desc  offset ? rows  fetch next ? rows only  ";
				arrSize++;
				arrSize++;
			}
			//qry = qry + " order by FORMAT(c.created_date, 'dd-MMM-yy'),FORMAT(c.created_date, 'HH:mm'),approver_type desc OFFSET "+obj.getPageIndex()+"0 ROWS FETCH NEXT 40 ROWS ONLY ";

			Object[] pValues = new Object[arrSize];
			int i = 0;
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getFrom_date();
				pValues[i++] = obj.getTo_date();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				pValues[i++] = obj.getSbu_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				pValues[i++] = obj.getProject_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				pValues[i++] = obj.getStatus();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				pValues[i++] = obj.getIncident_type();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getFrom_date();
				pValues[i++] = obj.getTo_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
				pValues[i++] = obj.getFrom_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getTo_date();
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getUser(); pValues[i++] = obj.getUser();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			pValues[i++] = obj.getAdmin_incidents();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				pValues[i++] = obj.getI_pending();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				pValues[i++] = obj.getI_completed();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getI_completed();
				pValues[i++] = obj.getUser();
				pValues[i++] = obj.getUser();
			}
			
			
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				pValues[i++] = obj.getSbu_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				pValues[i++] = obj.getProject_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				pValues[i++] = obj.getStatus();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				pValues[i++] = obj.getIncident_type();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getFrom_date();
				pValues[i++] = obj.getTo_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
				pValues[i++] = obj.getFrom_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getTo_date();
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getUser(); pValues[i++] = obj.getUser();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			pValues[i++] = obj.getAdmin_incidents();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				pValues[i++] = obj.getI_pending();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				pValues[i++] = obj.getI_completed();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getI_completed();
				pValues[i++] = obj.getUser();
				pValues[i++] = obj.getUser();
			}
			
			
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				pValues[i++] = obj.getSbu_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				pValues[i++] = obj.getProject_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				pValues[i++] = obj.getStatus();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				pValues[i++] = obj.getIncident_type();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getFrom_date();
				pValues[i++] = obj.getTo_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
				pValues[i++] = obj.getFrom_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getTo_date();
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getUser(); pValues[i++] = obj.getUser();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			pValues[i++] = obj.getAdmin_incidents();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				pValues[i++] = obj.getI_pending();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				pValues[i++] = obj.getI_completed();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getI_completed();
				pValues[i++] = obj.getUser();
				pValues[i++] = obj.getUser();
			}
				
			
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				pValues[i++] = obj.getSbu_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				pValues[i++] = obj.getProject_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				pValues[i++] = obj.getStatus();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				pValues[i++] = obj.getIncident_type();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getFrom_date();
				pValues[i++] = obj.getTo_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
				pValues[i++] = obj.getFrom_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getTo_date();
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getUser(); pValues[i++] = obj.getUser();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			pValues[i++] = obj.getAdmin_incidents();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				pValues[i++] = obj.getI_pending();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				pValues[i++] = obj.getI_completed();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getI_completed();
				pValues[i++] = obj.getUser();
				pValues[i++] = obj.getUser();
			}
			
				
			
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getSbu_code())) {
				pValues[i++] = obj.getSbu_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getProject_code())) {
				pValues[i++] = obj.getProject_code();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getIncident_type())) {
				pValues[i++] = obj.getIncident_type();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getStatus())) {
				pValues[i++] = obj.getStatus();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date()) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getFrom_date();
				pValues[i++] = obj.getTo_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getFrom_date())) {
				pValues[i++] = obj.getFrom_date();
			}
			else if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getTo_date())) {
				pValues[i++] = obj.getTo_date();
			}
			if(!StringUtils.isEmpty(obj) && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getUser(); pValues[i++] = obj.getUser();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getAdmin_incidents())) {
			pValues[i++] = obj.getAdmin_incidents();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_pending())) {
				pValues[i++] = obj.getI_pending();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())) {
				pValues[i++] = obj.getI_completed();
			}
			if(!StringUtils.isEmpty(obj) && !StringUtils.isEmpty(obj.getI_completed())  && (!CommonConstants.ADMIN.equals(obj.getRole()) && !CommonConstants.MANAGEMENT.equals(obj.getRole())) && !StringUtils.isEmpty(obj.getUser())) {
				pValues[i++] = obj.getI_completed();
				pValues[i++] = obj.getUser();
				pValues[i++] = obj.getUser();
			}
			if(!StringUtils.isEmpty(searchParameter)) {
				pValues[i++] = "%"+searchParameter+"%";
				pValues[i++] = "%"+searchParameter+"%";
				pValues[i++] = "%"+searchParameter+"%";
				pValues[i++] = "%"+searchParameter+"%";
				pValues[i++] = "%"+searchParameter+"%";
				pValues[i++] = "%"+searchParameter+"%";
				pValues[i++] = "%"+searchParameter+"%";
				pValues[i++] = "%"+searchParameter+"%";
				pValues[i++] = "%"+searchParameter+"%";
				pValues[i++] = "%"+searchParameter+"%";
				pValues[i++] = "%"+searchParameter+"%";
				pValues[i++] = "%"+searchParameter+"%";
				
			}
			if(!StringUtils.isEmpty(startIndex) && !StringUtils.isEmpty(offset)) {
				pValues[i++] = startIndex;
				pValues[i++] = offset;
			}
			objsList = jdbcTemplate.query( qry,pValues, new BeanPropertyRowMapper<IRM>(IRM.class));
			
			Set<String> nameSet = new HashSet<>();
			objsList = objsList.stream()
		            .filter(e -> nameSet.add(e.getDocument_code()))
		            .collect(Collectors.toList());
		}catch(Exception e){ 
			e.printStackTrace();
			throw new Exception(e);
		}
		return objsList;
	}
	
}
