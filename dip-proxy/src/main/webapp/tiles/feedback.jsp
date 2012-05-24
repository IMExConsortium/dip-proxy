<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<h1>Contact Us</h1>
<table width="98%" cellspacing="10">
 <tr>
  <td align="left" colspan="2">
   To send us comments, please complete the form below<br/>
   or send them directly to:
   <A HREF="mailto:dip@mbi.ucla.edu">dip@mbi.ucla.edu</A>
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
       <s:select name="about" list="#{'general':'General comment',
                                      'request':'Request a feature',
                                      'bug':'Bug report'}" />
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
       <s:textarea name="comment"  rows="8" cols="64"/>
      </td>
     </tr>
     <s:if test="#session['USER_ID'] <= 0" >
      <tr>
       <th width="10%"  align="right">Your E-mail</th>
       <td width="90%"  align="left">
        <s:textfield  name="email"  size="32" maxlength="80" />(optional)
       </td>
      </tr>
      <tr>
       <td>&nbsp;</td>
       <td align="left">
        <table cellpadding="0" cellspacing="0" >
         <tr>
          <th align="left" class="tcell">
           <br/>
           Type Both Words Below<br/>
           <s:if test="hasActionErrors()">
            <div id="errorDiv" style="padding-left: 10px; margin-bottom: 5px">
             <span class="error">
              <s:iterator value="actionErrors">
               <span class="errorMessage"><s:property escape="false" /></span>
             </s:iterator>
             </span>
            </div>
           </s:if>
          </th>
         </tr>
         <tr>
          <td align="left">
           <script type="text/javascript"
             src="https://api-secure.recaptcha.net/challenge?k=6LfVAwcAAAAAACFlY-X-icWpaoVMxHFyFVu3eIgc">
           </script>
           <noscript>
            <iframe src="https://api-secure.recaptcha.net/noscript?k=6LfVAwcAAAAAACFlY-X-icWpaoVMxHFyFVu3eIgc"
                height="300" width="500" frameborder="0">
            </iframe><br/>
            <textarea name="recaptcha_challenge_field" rows="3" cols="40"></textarea>
            <input type='hidden' name='recaptcha_response_field' value='manual_challenge' />
           </noscript>
          </td>
         </tr>
        </table>
       </td>
      </tr>
     </s:if>
     <tr>
      <td align="left" colspan="2">
       <s:submit name="submit" value="Submit" />
      </td>
     </tr>
    </table>
   </s:form>
  </td>
 </tr>
</table>
<br/>
<br/>
