<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

<center>
<table width="95%" cellspacing="3" cellpadding="3" border="1">
 <tr>
  <th colspan="5" align="left">
   Native Server Status
  </th>
 </tr>
 <tr>
  <th>Provider</th>
  <th>Service</th>
  <th width="260">Native Server Status</th>
  <th>Native Response Time [s]</th>
 </tr>

 <%--<s:iterator value="counts" var="prvd" status="ps"> --%>
 <s:iterator value="delays" var="prvd" status="ps">
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
         <td align="center"><img src="native-sparkline.action?prv=<s:property value='#prv'/>&srv=<s:property value='key'/>&range=day"/></td>
         <td align="center"><s:property value='delays[#prv][key]'/></td>
     </s:iterator>
    </tr>
  </s:if>
 </s:iterator>
</table>
</center>
