<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<%-- ===========================================================================
 ! $HeadURL:: http://wyu@imex.mbi.ucla.edu/svn/central/trunk/icentral/src/main#$
 ! $Id:: page.jsp 205 2011-05-16 00:49:36Z lukasz                              $
 ! Version: $Rev:: 205                                                         $
 !========================================================================= --%>

<s:set name="spath" value="%{skn}"/>

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
   <s:if test="source!=null">
     <tr> 
      <s:if test="page.showindex">
       <td rowspan="2" id="index-panel">
       </td>
      </s:if>
      <td width="99%" class="pagettl">
       <s:if test="page.showtitle">
        <h1><s:property value="page.title"/></h1>
       </s:if>
      </td>
      <td class="pagecom">
       <s:if test="page.showcomment"> 
        <t:insertDefinition name="pagecomments" />
       </s:if>
      </td>
      <td align="right">  
         <s:if test="#session['USER_ROLE'].administrator != null or
               #session['USER_ROLE'].editor != null">
           <s:form theme="simple" action="page">
            <s:hidden name="id" value="newpage"/>
            <s:hidden name="site" value="%{site}"/>  
            <s:submit theme="simple" name="dummy" value="NEW PAGE" />
           </s:form>
         </s:if>
      </td>
     </tr>
     <tr>
      <td colspan="3" class="page">
       <s:property value="source" escape="false" />
       <br/><br/><br/><br/><br/><br/><br/><br/><br/>
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

  <s:if test="page.showindex">
   <script>
     YAHOO.util.Event.addListener( window, "load",
       YAHOO.mbi.view.panel.index("<s:property value="page.urlindex" escape="false" />", 
                                   document.getElementById("index-panel")));     
   </script>
  </s:if>
 </body>
</html>
