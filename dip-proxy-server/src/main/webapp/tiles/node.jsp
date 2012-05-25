<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

<center>
<table width="95%" cellspacing="3" cellpadding="3" border="1">
 <tr>
  <th colspan="2" align="left">
   Node Status
  </th>
 </tr>
 <tr>
  <th width="10%">ID</th>
  <td><s:property value="nodeStatus['ID']"/></td>
 </tr>
 <tr>
  <th>Address:Port</th>
  <td><s:property value="nodeStatus['address']"/>:<s:property value="nodeStatus['port']"/></td>
 </tr>
 <tr>
  <th width="20%">Routing Algorithm</th>
  <td><s:property value="nodeStatus['algorithm']"/></td>
 </tr>
 <tr>
  <th width="20%">Routing Style</th>
  <td><s:property value="nodeStatus['style']"/></td>
 </tr>
 <tr>
  <th width="20%">Global Keys</th>
  <td><s:property value="nodeStatus['global keys']"/></td>
 </tr>
 <tr>
  <th width="20%">Local Keys</th>
  <td><s:property value="nodeStatus['local keys']"/></td>
 </tr>
</table>
<hr/>
<table width="95%" cellspacing="3" cellpadding="3" border="1">
 <tr>
  <th colspan="2" align="left">
   Routing Table
  </th>
 </tr>
 <tr>
  <td colspan="2">
    <s:property value="nodeStatus['routing table']" escape="false"/>
  </td>
 </tr>
</table>
</center>

