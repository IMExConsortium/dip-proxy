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
</center>
  <t:insertAttribute name="body"/>
<center>
  <s:if test="big">
    <t:insertTemplate template="/tiles/footer.jsp" flush="true">
     <t:putAttribute name="edit" value="/tiles/pageedit-yui.jsp" />
    </t:insertTemplate>
  </s:if>
</center>
</body>
</html>


