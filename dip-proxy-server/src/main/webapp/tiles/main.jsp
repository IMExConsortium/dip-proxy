<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
   <table width="100%" cellspacing="0" cellpadding="0" border="0">
    <tr>
     <td>
      <ul>
       <li>
        DIP Proxy Status
        <ul>
         <li><s:a action="cache-status">Local cache</s:a></li>
         <li><s:a action="native-status">Native servers</s:a></li>
        </ul>         
       </li>
      </ul>
     </td>
    </tr>
   </table>
