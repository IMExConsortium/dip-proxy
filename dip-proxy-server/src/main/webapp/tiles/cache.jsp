<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

<s:form theme="simple" action="cache-status">
<center>
<table width="95%" cellspacing="3" cellpadding="3" border="1">
 <tr>
  <th colspan="7" align="left">
    Native Cache Status
  </th>

 </tr>
 <tr>
  <th>Provider</th>
  <th>NativeCountAll</th>
  <th>NativeRemoveAll</th>
  <th>NativeExpireAll</th>
  <th>DxfCountAll</th>
  <th>DxfRemoveAll</th>
  <th>DxfExpireAll</th>
  <%--
  <th>Service</th>
  <th>Cache Entries</th> 
  --%>
 </tr>

 <s:iterator value="nativeCounts" >
   <s:set name="prv" value="key" />
   <tr>
 <%--<s:if test='nativeCounts.size > 0 '> --%>
   <%-- <s:iterator value="nativeCounts" var="prvd" status="ps"> --%>
 <%-- <s:iterator value="nativeCounts" status="ps"> -->
 
   <s:set name="prv" value="key" /> 

     <%--<th width="10%" rowspan="<s:property value='value.size'/>"> --%>
     <th width="10%">
       <s:property value="#prv"/>
     </th>
    
    
    <td>
        <s:property value="nativeCounts.get(#prv)"/>
    </td>

    <s:set name="nativeRemove" value="'op.nativeremove' + #prv"/>
    <s:set name="nativeExpire" value="'op.nativeexpire' + #prv"/>

    <td>
        <s:submit name="%{#nativeRemove}" value="RemoveAll"/>
    </td>      
    
    <td> 
        <s:submit name="%{#nativeExpire}" value="ExpireAll"/>
    </td>
     <%--
     <s:iterator value="value" var="srvc" status="ss">
       <s:if test='#ss.index > 0'>
         </tr>
         <tr>
       </s:if>
         <td><s:property value="key"/></td>
         <td><s:property value="value"/></td>
     </s:iterator>
    </tr>
     --%>
    
   <td>
        <s:property value="dxfCounts.get(#prv)"/>
   </td>

    <s:set name="dxfRemove" value="'op.dxfremove' + #prv"/>
    <s:set name="dxfExpire" value="'op.dxfexpire' + #prv"/>

    <td>
        <s:submit name="%{#dxfRemove}" value="RemoveAll"/>
    </td>

    <td> 
        <s:submit name="%{#dxfExpire}" value="ExpireAll"/>
   </td>
     <%--
     <s:iterator value="value" var="srvc" status="ss">
       <s:if test='#ss.index > 0'>
         </tr>
         <tr>
       </s:if>
         <td><s:property value="key"/></td>
         <td><s:property value="value"/></td>
     </s:iterator>
     --%>
    </tr>
 </s:iterator>

</table>

</center>

</s:form>
