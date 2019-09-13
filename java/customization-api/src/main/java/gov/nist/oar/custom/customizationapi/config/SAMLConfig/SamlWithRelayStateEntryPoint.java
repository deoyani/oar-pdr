package gov.nist.oar.custom.customizationapi.config.SAMLConfig;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.SAMLEntryPoint;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.security.saml.websso.WebSSOProfileOptions;

/***
 * This helps SAML endpoint to redirect after successful login service.
 * 
 * @author Deoyani Nandrekar-Heinis
 *
 */
public class SamlWithRelayStateEntryPoint extends SAMLEntryPoint {

    public SamlWithRelayStateEntryPoint() {

    }

    private String relaystate = "";

    public SamlWithRelayStateEntryPoint(String connectingapp) {
	this.relaystate = connectingapp;
    }

    @Override
    protected WebSSOProfileOptions getProfileOptions(SAMLMessageContext context, AuthenticationException exception) {

	WebSSOProfileOptions ssoProfileOptions;
	if (defaultOptions != null) {
	    ssoProfileOptions = defaultOptions.clone();
	} else {
	    ssoProfileOptions = new WebSSOProfileOptions();
	}

//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (!(authentication instanceof AnonymousAuthenticationToken)) {
//            String currentUserName = authentication.getName();
//            System.out.println("****** TEST ***** +"+currentUserName);
//        }
//        System.out.println("****** TEST ***** +"+context);

	// Not :
	// Add your custom logic here if you need it.
	// Original HttpRequest can be extracted from the context param
	// So you can let the caller pass you some special param which can be used to
	// build an on-the-fly custom
	// relay state param

	// ssoProfileOptions.setRelayState("http://localhost:4200");
	ssoProfileOptions.setRelayState(this.relaystate);
//        ssoProfileOptions.setRelayState("https://inet.nist.gov/");
	return ssoProfileOptions;
    }

}