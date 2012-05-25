<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="t" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

<h1>Sign up for ImexCentral Account</h1>
<table width="98%">
 <tr>
  <td align=left>
   <font size=-2>
    Already have an account? <A HREF="user">Log in</A>
   </font>
  </td>
 </tr>
 <tr>
  <td align="left">
   <font size="-2"> 
    Forgotten your password? <A HREF="mailto:dip@mbi.ucla.edu">Contact Us</A>
   </font>
  </td>
 </tr>
 <tr>
  <td align="center">
   <table width="98%" class="form_descr">
    <tr>
     <td align="left">
       In orer to gain full access to ImexCentral you must register.
     </td>
    </tr>
   </table>
  </td>
 </tr>
 <tr>
  <td align="center" width="98%">
   <s:form action="register"><tr><td align="center">
    <s:hidden theme="simple" name="op" value="reg" />
    <table width="85%" cellpadding="0" cellspacing="2">
     <tr>
      <td class="clogin">
       <table width="100%">
        <tr>
         <td align="center"><b>
          User Registraton - all fields required</b>
         </td>
        </tr>
       </table>
      </td>
     </tr>

     <tr>
      <td align="center">
       <table width="95%" cellpadding="0" cellspacing="0">
        <tr>
         <td align="center" colspan="1" class="clogin1">
          <table width="98%" cellspacing="1" cellpadding="3">
           <tr>
            <td class="tcell" align="center" colspan="3">
             <b>User Name</b>
             <s:if test="hasFieldErrors()">
               <s:if test="fieldErrors['user.login']!=null">  
                <div id="errorDiv" style="padding-left: 10px; margin-bottom: 5px">
                 <span class="error">
                  <span class="errorMessage">
                   <s:property value="fieldErrors['user.login'][0]" />
                  </span>
                 </span>
                </div>
               </s:if>
              </s:if>
             <s:textfield theme="simple" 
                           name="user.login" size="32" maxlength="32" />
              <s:if test="hasFieldErrors()">
               <s:if test="fieldErrors['pass1']!=null">  
                <div id="errorDiv" style="padding-left: 10px; margin-bottom: 5px">
                 <span class="error">
                  <span class="errorMessage">
                   <s:property value="fieldErrors['pass1'][0]" />
                  </span>
                 </span>
                </div>
               </s:if>
              </s:if>
            </td>
           </tr>
           <tr>
            <td width="50%" align="right">
               <b>Password</b>
               <s:password theme="simple"
                            name="pass0" size="24" maxlength="24" />
            </td>  
            <td>&nbsp;</td>
            <td width="50%" align="left">
               <b>Password</b> (retype) 
               <s:password theme="simple"
                           name="pass1" size="24" maxlength="24" />
            </td>
           <tr>
          </table>
         </td>
        </tr>
       </table>
      </td>  
     </tr>
      
     <tr>
      <td align="center">
       <table width="95%" cellpadding="0" cellspacing="0">    
        <tr>
         <td align="center" colspan="1" class="clogin1">
          <table width="98%" cellspacing="1" cellpadding="3">

           <s:textfield label="First Name" labelSeparator=""
                        name="user.firstName" size="32" maxlength="64" />
           <s:textfield label="Last Name" labelSeparator=""
                        name="user.lastName" size="32" maxlength="64" />
           <s:textfield label="Organization" labelSeparator=""
                        name="user.affiliation" size="32" maxlength="64" />
           <s:textfield label="E-Mail" labelSeparator=""
                        name="user.email" size="32" maxlength="64" />
           	
           <s:if test="hasFieldErrors()">
            <s:if test="fieldErrors['agree']!=null">  
             <tr>
              <td colspan="2" align="center">
               <div id="errorDiv" style="padding-left: 10px; margin-bottom: 5px">
                 <span class="error">
                  <span class="errorMessage">
                   <s:property value="fieldErrors['agree'][0]" />
                  </span>
                 </span>
               </div>
              </td>
             </tr>
            </s:if>
           </s:if>

           <tr>
            <td align="right" class="tcell">TERMS OF USE</td>
            <td align="left" class="tcell">                        
             <font size=-1>
              <table width="%80" class="tou">
               <tr>
                <td>
                 <s:checkbox theme="simple" name="agree" value="false"/> 
                </td>
                <td>
                 I represent that I have read and accepted the  
                 <A HREF="page?id=termsofuse&mdf=0:2:0&mst=3:0:0"><nobr>Terms of Use</nobr></A>.
                </td>
               </tr>
              </table>
             </font>
            </td>
           </tr>
          </table>
         </td>
        </tr>
       </table>
      </td>
     </tr>

     <tr>
      <td align="center">
        <table width="95%" cellpadding="0" cellspacing="0">
         <tr> 
          <td class="captcha1" colspan="2" align="center">
           <table cellpadding="0" cellspacing="0">
            <tr>
             <th align="left" class="tcell">
              Type Both Words Below
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
             <td>
              <script type="text/javascript"                             
                 src="https://api-secure.recaptcha.net/challenge?k=${recaptcha.publicKey}">
              </script>
              <noscript>
               <iframe src="https://api-secure.recaptcha.net/noscript?k=${recaptcha.publicKey}" 
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
        </table>
      </td>
     </tr>
     
     <tr>
      <td align="left" CLASS="clogin">
       <table cellpadding="5">
        <tr>
         <td>
          <s:submit theme="simple" name="SUBMIT" value="SUBMIT" />
         </td>
         <td>
          <font size=-1>
           You will be notified about account
           creation and the initial password by an <i>e-mail</i>
           sent to the address specified above.
          </font>
         </td>
        </tr>
       </table>
      </td>
     </tr>

    </table>
   </td></tr></s:form>      
  </td>
 </tr>
 <tr>
  <td> 
   <br/>
  </td>
 </tr>
</table>

