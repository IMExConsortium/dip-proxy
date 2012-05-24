<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

<center>
<table width="95%" cellspacing="3" cellpadding="3" border="1">
 <tr>
  <th colspan="5" align="left">
   Cache Status
  </th>
 </tr>
 <tr>
  <th>Provider</th>
  <th>Service</th>
  <th>Cache Entries</th>
 </tr>

 <s:iterator value="counts" var="prvd" status="ps">
  <s:if test='value.size >0'>
   <s:set name="prv" value="key" /> 
    <tr>
     <th width="10%" rowspan="<s:property value='value.size'/>">
       <s:property value="key"/>
     </th>
           
     <s:iterator value="value" var="srvc" status="ss">
       <s:if test='#ss.index > 0'>
         </tr>
         <tr>
       </s:if>
         <td><s:property value="key"/></td>
         <td><s:property value="value"/></td>
     </s:iterator>
    </tr>
  </s:if>
 </s:iterator>
</table>
</center>
