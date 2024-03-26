<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>menu</title>
<style>


div.main-menu-content {
  position: -webkit-sticky;
  position: sticky;
 
}
</style>
</head>
<body>
		<div class="navbar-container main-menu-content" data-menu="menu-container" id="menu">
          <ul class="nav navbar-nav" id="main-menu-navigation" data-menu="menu-navigation" >
          
       <%--  <c:if test="${sessionScope.BASE_ROLE ne 'User' }"> --%>
           <li class="dropdown nav-item "  data-menu id="home"  url ="/home">
					    <a class="dropdown-item d-flex align-items-center" href="<%=request.getContextPath() %>/reone/home" data-bs-toggle=""
					    data-i18n="Analytics"> <i data-feather="home"></i>
					      <span data-i18n="Dashboards">Dashboard</span></a>
			</li>
          <%--  </c:if>  --%>
          
	
		<c:if test="${sessionScope.R_DASHBOARD eq 'Yes' }">
            <li class="dropdown nav-item " data-menu="dropdown" id="masters"><a class="dropdown-toggle nav-link d-flex align-items-center" href="#" data-bs-toggle="dropdown"><i data-feather="package"></i><span data-i18n="Masters">Masters</span></a>
              <ul class="dropdown-menu" data-bs-popper="none">
               <c:forEach var="obj" items="${menuList}" varStatus="index">
					<li class="" data-menu  id="mastersChild${index.count }" url = "${obj.module_url }" onClick="exFunction('mastersChild${index.count }')">
					    <a class="dropdown-item d-flex align-items-center" href="<%=request.getContextPath() %>/reone${obj.module_url }" data-bs-toggle=""
					     data-i18n="${obj.module_name }"><svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="feather feather-circle"><circle cx="12" cy="12" r="10"></circle></svg>
					      <span data-i18n="${obj.module_name }">${obj.module_name }</span></a> 
					</li>
				</c:forEach>
            </ul>
           </li>
        </c:if>
             <li class="dropdown nav-item " data-menu id="irm" url="irm">
			    <a class="dropdown-item d-flex align-items-center" href="<%=request.getContextPath() %>/reone/irm" data-bs-toggle=""
			     data-i18n="Analytics"><svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none"
			      stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="feather feather-activity">
			      <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline></svg>
			      <span data-i18n="Analytics">Incident Report</span></a>
	         </li>
			<%-- <c:if test="${sessionScope.R_DASHBOARD eq 'Yes' }">
			 <li class="dropdown nav-item " data-menu id="home">
			    <a class="dropdown-item d-flex align-items-center" href="<%=request.getContextPath() %>/reone/home" data-bs-toggle=""
			     data-i18n="Analytics"><svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none"
			      stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="feather feather-activity">
			      <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline></svg>
			      <span data-i18n="Analytics">Work Permit</span></a>
			</li>
			</c:if> --%>
			 <li class="dropdown nav-item " data-menu id="reports" url="irm-report">
			    <a class="dropdown-item d-flex align-items-center" href="<%=request.getContextPath() %>/reone/irm-report" data-bs-toggle=""
			     data-i18n="Analytics"><i data-feather='layers'></i>
			      <span data-i18n="Analytics">Reports</span></a>
			</li>
			
			<li class="dropdown nav-item " data-menu id="help" url="help">
			    <a class="dropdown-item d-flex align-items-center" href="<%=request.getContextPath() %>/reone/help-center" data-bs-toggle=""
			     data-i18n="Analytics"><i data-feather='help-circle'></i>
			      <span data-i18n="Analytics">Help Center</span></a>
			</li>
			
	<%-- 	<li class="dropdown nav-item " data-menu id="home">
			    <a class="dropdown-item d-flex align-items-center" href="<%=request.getContextPath() %>/reone/bb-is" data-bs-toggle=""
			     data-i18n="Analytics"><i data-feather='codesandbox'></i>
			      <span data-i18n="Analytics">BrainBox </span></a>
			</li>  --%>
				
          </ul>
        </div>
        <script>
        var option = window.localStorage.getItem("selectedOption");
        var child_option = window.localStorage.getItem("selectedOptionChild");
        $( document ).ready(function() {
        	
        	var url = $(location).attr('href');
        	//var url = 	$("#"+option).attr('url')
        		if(option != 'undefined' && option != null){
            		$('li.active').removeClass('active');
            		if(url.indexOf('/irm-report') != -1){
           			 $('#reports').addClass('active');
		           		}else if(url.indexOf('/irm') != -1 || url.indexOf('/update-irm-form') != -1){
		           			$('#irm').addClass('active');
		           		}else if(url.indexOf('/home') != -1){
		           			$('#home').addClass('active');
		           		}else if(url.indexOf('/help') != -1){
		           			$('#help').addClass('active');
		           		}else if(option.indexOf('masters') != -1 || option == 'irm'){
		           			$('#masters').addClass('active');
		           			$('#'+option+'li.active').removeClass('active');
		       	            $('#'+child_option).addClass('active');
		           		}else{
		           			$('#home').addClass('active');
		           		}
       	          /*  if(option == 'masters'){
       	        	$('#'+option+'li.active').removeClass('active');
       	            $('#'+child_option).addClass('active');
       	           } */
            	}else{
            		$('li.active').removeClass('active');
            		if(url.indexOf('/irm-report') != -1){
              			 $('#reports').addClass('active');
   		           		}else if(url.indexOf('/irm') != -1 || url.indexOf('/update-irm-form') != -1){
   		           			$('#irm').addClass('active');
   		           		}else if(url.indexOf('/home') != -1){
   		           			$('#home').addClass('active');
   		           		}else if(url.indexOf('/help') != -1){
   		           			$('#help').addClass('active');
   		           		}else if(option.indexOf('masters') != -1){
   		           			$('#masters').addClass('active');
   		           			$('#'+option+'li.active').removeClass('active');
   		       	            $('#'+child_option).addClass('active');
   		           		}else{
   		           			$('#home').addClass('active');
   		           		}
            	}
  	        <%--  window.location.href= "<%=request.getContextPath()%>"+"/"+url;  --%>
        	
        });
        $('#menu ul li ').on('click', function(){
            $('li.active').removeClass('active');
            $(this).addClass('active');
            
            window.localStorage.setItem("selectedOption", $(this).attr('id'));
        }); 
        function exFunction(id) {  window.localStorage.setItem("selectedOptionChild", id);  }
        </script>
</body>
</html>