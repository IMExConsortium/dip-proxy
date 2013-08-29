<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

<s:form theme="simple" action="cache-status">
<center>
<table width="95%" cellspacing="3" cellpadding="3" border="1">
 <tr>
  <th colspan="8" align="left">
    Native Cache Status
  </th>

 </tr>
 <tr>
  <th>Provider</th>
  <th>Service</th>
  <th>NativeCount</th>
  <th>DxfCount</th>
  <th>NativeRemoveAll</th>
  <th>NativeExpireAll</th>
  <th>DxfRemoveAll</th>
  <th>DxfExpireAll</th> 
 </tr>

 
 <s:iterator value="nativeCounts" >
   <s:set name="prv" value="key" />
   <s:set name="serviceMap" value="value"/>
   <s:set name="rowNum" value="value.size()" />

   <s:set name="nativeRemove" value="'op.nativeremove' + #prv"/>
   <s:set name="nativeExpire" value="'op.nativeexpire' + #prv"/>
   <s:set name="dxfRemove" value="'op.dxfremove' + #prv"/>
   <s:set name="dxfExpire" value="'op.dxfexpire' + #prv"/>
    
   <tr>
     <th width="10%" rowspan="<s:property value='#rowNum'/>"> 
       <s:property value="#prv"/>
     </th>

    <s:iterator value="serviceMap" status="ss">
       <s:set name="service" value="key"/>
       <s:set name="nativeCount" value="value"/>
       
       <s:if test='#ss.index == 0'>
         <td><s:property value="#service"/></td>
         <td><s:property value="#nativeCount"/></td>
         <td><s:property value="dxfCounts.get(#prv).get(#service)"/></td>

         <td rowspan="<s:property value='#rowNum'/>"> 
            <s:submit name="%{#nativeRemove}" value="RemoveAll"/>
         </td>
    
         <td rowspan="<s:property value='#rowNum'/>">
            <s:submit name="%{#nativeExpire}" value="ExpireAll"/>
         </td>
       
         <td rowspan="<s:property value='#rowNum'/>">
            <s:submit name="%{#dxfRemove}" value="RemoveAll"/>  
         </td>

         <td rowspan="<s:property value='#rowNum'/>"> 
            <s:submit name="%{#dxfExpire}" value="ExpireAll"/>
         </td>
     </tr>
       </s:if>
       <s:else>
         <tr>
            <td><s:property value="#service"/></td>
            <td><s:property value="#nativeCount"/></td>   
            <td><s:property value="dxfCounts.get(#prv).get(#service)"/></td>
         </tr>
       </s:else> 
     </s:iterator>
 </s:iterator>
 
</table>

</center>

</s:form>
