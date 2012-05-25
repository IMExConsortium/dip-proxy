<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

<table width="100%">
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
   </table>  
  </td>
 </tr>

<tr>
  <td>
   <center>
    <s:form theme="simple" action="aclmgr">
     <fieldset class="qfield">
      <legend class="qlegend">Reinitialize ACL</legend>
      <table width="100%" class="qtable">
       <tr>
        <td align="left">
         <input type="submit" id="aclmgr_op_ini" name="op.rin" value="REINITIALIZE" tabindex="3"/>
        </td>
       </tr>
      </table>
     </fieldset>
    </s:form>
   </center>
  </td>
 </tr>

 </table>
 <br/>
 <br/>
