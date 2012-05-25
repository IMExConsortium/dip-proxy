<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

<h1>Login into ImexCentral Account</h1>
<table width="98%">
 <tr>
  <td align="left">
   <font size="-2"> 
    Forgotten your password? <A HREF="mailto:dip@mbi.ucla.edu">Contact Us</A>
   </font>
  </td>
 </tr>
 <tr>
  <td align="left">
   <br/>
   <table width="66%" cellspacing="1" cellpadding="3">
    <s:if test="hasActionErrors()">
     <tr><td>  
      <div id="errorDiv" style="padding-left: 10px; margin-bottom: 5px">
       <span class="error">
        <s:iterator value="actionErrors">
         <span class="errorMessage"><s:property escape="false" /></span>
        </s:iterator>
       </span>
      </div>
     </td></tr>
    </s:if>

    <s:form theme="simple" action="user"> 
    <s:hidden theme="simple" name="op" value="login" />
    <tr>
     <td align="right" class="tcell">User Name</td>
     <td align="left" class="tcell">
      <s:textfield theme="simple" name="user.login" size="16" maxlength="16" />
     </td>
    </tr>
    <tr>
     <td align="right" class="tcell">Password</td>
     <td align="left" class="tcell">
      <s:password theme="simple" name="pass0" size="16" maxlength="16" />
     </td>
    </tr>
    <tr>
     <td align="right" class="tcell">&nbsp;</td>
     <td align="left" class="tcell">
       <s:submit theme="simple" name="LOGIN" value="LOGIN" />
     </td>
    </tr>
    </s:form>
   </table>
   <br/><br/><br/><br/><br/>
   <br/><br/><br/><br/><br/>
   <br/><br/><br/><br/><br/>
   <br/><br/><br/><br/><br/>
   <br/><br/><br/><br/><br/>
   <br/><br/><br/><br/><br/>
  </td>
 </tr>
</table>
