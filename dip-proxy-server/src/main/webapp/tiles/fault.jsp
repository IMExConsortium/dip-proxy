<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%-- ===========================================================================
 ! $HeadURL::                                                                  $
 ! $Id::                                                                       $
 ! Version: $Rev::                                                             $
 !========================================================================= --%>

<s:set var="spath" value="%{skn}"/>
<html lang="en">
 <head>
  <meta http-equiv="content-type" content="text/html; charset=utf-8">
  <title><s:property value="page.title"/></title>
  <t:insertDefinition name="htmlhead"/>
  <script src="js/modal-yui.js" type="text/javascript" language="JavaScript"></script>
  <script src="js/help-yui.js" type="text/javascript" language="JavaScript"></script>
  <script src="js/side-panel-yui.js" type="text/javascript" language="JavaScript"></script>
 </head>
 <body class="yui-skin-sam" onLoad="var nos = document.getElementById('noscript'); if ( nos !== null ) { nos.innerHTML='';}">
  <center>
  <s:if test="big">
   <t:insertTemplate template="/tiles/header.jsp" flush="true"/>
  </s:if>

  <table class="pagebody" width="100%" cellspacing="0" cellpadding="0"> 
  <s:if test="hasActionErrors()">
    <tr>
     <td colspan="3">
      <div  class="upage" id="errorDiv">
       <span class="pgerror">
        <s:iterator value="actionErrors">
         <span class="errorMessage"><s:property escape="false" /></span>
        </s:iterator>
       </span>
      </div>
      <br/>
     </td>
    </tr>
   </s:if>
 </table>
 <s:if test="big">
   <t:insertTemplate template="/tiles/footer.jsp" flush="true">
    <t:putAttribute name="edit" value="/tiles/pageedit-yui.jsp" />
   </t:insertTemplate>
  </s:if>
 </center>

 
</body>
</html>
