<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %> 

<s:set name="spath" value="%{skn}"/>
<html lang="en">
 <t:insertDefinition name="htmlhead"/>
 <body class="yui-skin-sam" onLoad="var nos = document.getElementById('noscript'); if ( nos !== null ) { nos.innerHTML='';}">
  <center>

  <s:if test="big">
   <t:insertTemplate template="/tiles/header.jsp" flush="true"/>
  </s:if>

  <t:insertAttribute name="body"/>

  <s:if test="big">
   <t:insertTemplate template="/tiles/footer.jsp" flush="true">
    <t:putAttribute name="edit" value="/tiles/pageedit-yui.jsp" />
   </t:insertTemplate>
  </s:if>
</center>
</body>
</html>

<%--
<HTML>
 <HEAD>
  <link rel="stylesheet" href="css/dip2.css" type="text/css" title="dip2">
  <link rel="stylesheet" href="css/dip2tab.css" type="text/css" title="dip2">
  <TITLE>DIP Proxy</TITLE>
  <s:head/>
 </HEAD>

 <BODY onLoad="self.name='DIP_MA'; self.focus()">
  <SCRIPT TYPE="text/javascript" SRC="script/proxy.js" LANGUAGE="JavaScript"></SCRIPT>
  <center>
   <t:insertAttribute name="header" />
   <hr/> 
   <t:insertAttribute name="body" />
   <hr/>
   <t:insertAttribute name="footer" />
  </center>
 </BODY>
</HTML>
--%>
