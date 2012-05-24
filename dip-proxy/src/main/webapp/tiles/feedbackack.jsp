<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<h1>Contact Us</h1>
<table width="98%" cellspacing="10">
 <tr>
  <td align="left" colspan="2">
   Thank you for sumitting your comments.<br/>
   <br/>
   <br/>
  </td>
 </tr>
 <tr>
  <td colspan="1" align="center">
   <s:form action="feedback" theme ="simple">
    <table width="96%">
     <tr>
      <th width="10%"  align="right">Subject</th>
      <td width="90%"  align="left">
       <i><s:property value="about"/></i>
      </td>
     </tr>
     <tr>
      <th width="10%" colspan="1" align="right">Comments</th>
      <td width="90%" colspan="2"  align="left">
       <s:if test="hasFieldErrors()">
        <s:if test="fieldErrors['comment']!=null">
         <div id="errorDiv" style="padding-left: 10px; margin-bottom: 5px">
          <span class="error">
           <span class="errorMessage">
            <s:property value="fieldErrors['comment'][0]" />
           </span>
          </span>
         </div>
        </s:if>
       </s:if>
       <s:textarea name="comment"  rows="8" cols="64" disabled="true"/>
      </td>
     </tr>
     <s:if test="#session['USER_ID'] <= 0" >
      <s:if test="email != null">
       <tr>
        <th width="10%"  align="right" nowrap>Your E-mail</th>
        <td width="90%"  align="left">
         <s:property value="email"/>
        </td>
       </tr>
      </s:if>
     </s:if>
    </table>
   </s:form>
  </td>
 </tr>
</table>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
