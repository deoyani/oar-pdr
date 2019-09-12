package gov.nist.oar.custom.customizationapi.config.SAMLConfig;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.saml2.metadata.provider.ResourceBackedMetadataProvider;
import org.opensaml.util.resource.ClasspathResource;
import org.opensaml.util.resource.ResourceException;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml.*;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.context.SAMLContextProviderLB;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.log.SAMLDefaultLogger;
import org.springframework.security.saml.metadata.CachingMetadataManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate;
import org.springframework.security.saml.metadata.MetadataDisplayFilter;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataGeneratorFilter;
import org.springframework.security.saml.parser.ParserPoolHolder;
import org.springframework.security.saml.processor.HTTPPostBinding;
import org.springframework.security.saml.processor.HTTPRedirectDeflateBinding;
import org.springframework.security.saml.processor.SAMLBinding;
import org.springframework.security.saml.processor.SAMLProcessorImpl;
import org.springframework.security.saml.trust.httpclient.TLSProtocolConfigurer;
import org.springframework.security.saml.trust.httpclient.TLSProtocolSocketFactory;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.security.saml.util.VelocityFactory;
import org.springframework.security.saml.websso.SingleLogoutProfile;
import org.springframework.security.saml.websso.SingleLogoutProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfile;
import org.springframework.security.saml.websso.WebSSOProfileConsumer;
import org.springframework.security.saml.websso.WebSSOProfileConsumerHoKImpl;
import org.springframework.security.saml.websso.WebSSOProfileConsumerImpl;
import org.springframework.security.saml.websso.WebSSOProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfileOptions;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import gov.nist.oar.custom.customizationapi.service.SamlUserDetailsService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Deoyani Nandrekar-Heinis
 */
@Configuration
public class SecuritySamlConfig extends WebSecurityConfigurerAdapter {

    @Value("${saml.metdata.entityid:testid}")
    String entityId;
    
    @Value("${saml.metadata.entitybaseUrl:testurl}")
    String entityBaseURL;
    
    @Value("${saml.keystore.path:testpath}")
    String keyPath;
    
    @Value("${saml.keystroe.storepass:testpass}")
    String keystorePass;
    
    @Value("${saml.keystore.key:testkey}")
    String keyAlias;
    
    @Value("${saml.keystore.keypass:keypass}")
    String keyPass;
    
    
    @Value("${auth.federation.metadata:fedmetadata}")
    String federationMetadata;
    @Value("${saml.scheme:samlscheme}")
    String samlScheme;
    @Value("${saml.server.name:keypass}")
    String samlServer;
    @Value("${saml.server.context-path:keypass}")
    String samlContext;
    
    @Bean
    public WebSSOProfileOptions defaultWebSSOProfileOptions() {
	WebSSOProfileOptions webSSOProfileOptions = new WebSSOProfileOptions();
	webSSOProfileOptions.setIncludeScoping(false);
	// Relay state can also be set here
	// webSSOProfileOptions.setRelayState("https://data.nist.gov/sdp");
	return webSSOProfileOptions;
    }

    @Bean
    public SAMLEntryPoint samlEntryPoint() {
	SAMLEntryPoint samlEntryPoint = new SamlWithRelayStateEntryPoint();
	samlEntryPoint.setDefaultProfileOptions(defaultWebSSOProfileOptions());
	return samlEntryPoint;
    }

    @Bean
    public MetadataDisplayFilter metadataDisplayFilter() {
	return new MetadataDisplayFilter();
    }

    @Bean
    public SimpleUrlAuthenticationFailureHandler authenticationFailureHandler() {
	return new SimpleUrlAuthenticationFailureHandler();
    }

    @Bean
    public SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler() {
	SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler = new SAMLRelayStateSuccessHandler();
	return successRedirectHandler;
    }

    @Bean
    public SAMLProcessingFilter samlWebSSOProcessingFilter() throws Exception {
	SAMLProcessingFilter samlWebSSOProcessingFilter = new SAMLProcessingFilter();
	samlWebSSOProcessingFilter.setAuthenticationManager(authenticationManager());
	samlWebSSOProcessingFilter.setAuthenticationSuccessHandler(successRedirectHandler());
	samlWebSSOProcessingFilter.setAuthenticationFailureHandler(authenticationFailureHandler());
	return samlWebSSOProcessingFilter;
    }

    @Bean
    public HttpStatusReturningLogoutSuccessHandler successLogoutHandler() {
	return new HttpStatusReturningLogoutSuccessHandler();
    }

    @Bean
    public SecurityContextLogoutHandler logoutHandler() {
	SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
	logoutHandler.setInvalidateHttpSession(true);
	logoutHandler.setClearAuthentication(true);
	return logoutHandler;
    }

    @Bean
    public SAMLLogoutFilter samlLogoutFilter() {
	return new SAMLLogoutFilter(successLogoutHandler(), new LogoutHandler[] { logoutHandler() },
		new LogoutHandler[] { logoutHandler() });
    }

    @Bean
    public SAMLLogoutProcessingFilter samlLogoutProcessingFilter() {
	return new SAMLLogoutProcessingFilter(successLogoutHandler(), logoutHandler());
    }

    @Bean
    public MetadataGeneratorFilter metadataGeneratorFilter() {
	return new MetadataGeneratorFilter(metadataGenerator());
    }

    @Bean
    public MetadataGenerator metadataGenerator() {
	MetadataGenerator metadataGenerator = new MetadataGenerator();
	metadataGenerator.setEntityId(entityId);
	metadataGenerator.setEntityBaseURL(entityBaseURL);
	metadataGenerator.setExtendedMetadata(extendedMetadata());
	metadataGenerator.setIncludeDiscoveryExtension(false);
	metadataGenerator.setKeyManager(keyManager());
	return metadataGenerator;
    }

    @Bean
    public KeyManager keyManager() {
	ClassPathResource storeFile = new ClassPathResource(keyPath);
	String storePass = keystorePass;
	Map<String, String> passwords = new HashMap<>();
	passwords.put(keyAlias, keyPass);
	return new JKSKeyManager(storeFile, storePass, passwords, keyAlias);
    }

    @Bean
    public ExtendedMetadata extendedMetadata() {
	ExtendedMetadata extendedMetadata = new ExtendedMetadata();
	extendedMetadata.setIdpDiscoveryEnabled(false);
	extendedMetadata.setSignMetadata(false);
	return extendedMetadata;
    }

    @Bean
    public FilterChainProxy samlFilter() throws Exception {
	List<SecurityFilterChain> chains = new ArrayList<>();

	chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/metadata/**"),
		metadataDisplayFilter()));

	chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/login/**"), samlEntryPoint()));

	chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SSO/**"),
		samlWebSSOProcessingFilter()));

	chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/logout/**"), samlLogoutFilter()));

	chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SingleLogout/**"),
		samlLogoutProcessingFilter()));

	return new FilterChainProxy(chains);
    }

    @Bean
    public TLSProtocolConfigurer tlsProtocolConfigurer() {
	return new TLSProtocolConfigurer();
    }

    @Bean
    public ProtocolSocketFactory socketFactory() {
	return new TLSProtocolSocketFactory(keyManager(), null, "default");
    }

    @Bean
    public Protocol socketFactoryProtocol() {
	return new Protocol("https", socketFactory(), 443);
    }

    @Bean
    public MethodInvokingFactoryBean socketFactoryInitialization() {
	MethodInvokingFactoryBean methodInvokingFactoryBean = new MethodInvokingFactoryBean();
	methodInvokingFactoryBean.setTargetClass(Protocol.class);
	methodInvokingFactoryBean.setTargetMethod("registerProtocol");
	Object[] args = { "https", socketFactoryProtocol() };
	methodInvokingFactoryBean.setArguments(args);
	return methodInvokingFactoryBean;
    }

    @Bean
    public VelocityEngine velocityEngine() {
	return VelocityFactory.getEngine();
    }

    @Bean(initMethod = "initialize")
    public StaticBasicParserPool parserPool() {
	return new StaticBasicParserPool();
    }

    @Bean(name = "parserPoolHolder")
    public ParserPoolHolder parserPoolHolder() {
	return new ParserPoolHolder();
    }

    @Bean
    public HTTPPostBinding httpPostBinding() {
	return new HTTPPostBinding(parserPool(), velocityEngine());
    }

    @Bean
    public HTTPRedirectDeflateBinding httpRedirectDeflateBinding() {
	return new HTTPRedirectDeflateBinding(parserPool());
    }

    @Bean
    public SAMLProcessorImpl processor() {
	Collection<SAMLBinding> bindings = new ArrayList<>();
	bindings.add(httpRedirectDeflateBinding());
	bindings.add(httpPostBinding());
	return new SAMLProcessorImpl(bindings);
    }

    @Bean
    public HttpClient httpClient() {
	return new HttpClient(multiThreadedHttpConnectionManager());
    }

    @Bean
    public MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager() {
	return new MultiThreadedHttpConnectionManager();
    }

    @Bean
    public static SAMLBootstrap sAMLBootstrap() {
	return new SAMLBootstrap();
    }

    @Bean
    public SAMLDefaultLogger samlLogger() {
	return new SAMLDefaultLogger();
    }

    @Bean
    public SAMLContextProviderImpl contextProvider() {
	SAMLContextProviderLB samlContextProviderLB = new SAMLContextProviderLB();
	samlContextProviderLB.setScheme(samlScheme);
	samlContextProviderLB.setServerName(samlServer);
	samlContextProviderLB.setServerPort(443);
	samlContextProviderLB.setIncludeServerPortInRequestURL(true);
	samlContextProviderLB.setContextPath(samlContext);
	samlContextProviderLB.setStorageFactory(new org.springframework.security.saml.storage.EmptyStorageFactory());
	return samlContextProviderLB;
    }

    // SAML 2.0 WebSSO Assertion Consumer
    @Bean
    public WebSSOProfileConsumer webSSOprofileConsumer() {
	return new WebSSOProfileConsumerImpl();
    }

    // SAML 2.0 Web SSO profile
    @Bean
    public WebSSOProfile webSSOprofile() {
	return new WebSSOProfileImpl();
    }

    // not used but autowired...
    // SAML 2.0 Holder-of-Key WebSSO Assertion Consumer
    @Bean
    public WebSSOProfileConsumerHoKImpl hokWebSSOprofileConsumer() {
	return new WebSSOProfileConsumerHoKImpl();
    }

    // not used but autowired...
    // SAML 2.0 Holder-of-Key Web SSO profile
    @Bean
    public WebSSOProfileConsumerHoKImpl hokWebSSOProfile() {
	return new WebSSOProfileConsumerHoKImpl();
    }

    @Bean
    public SingleLogoutProfile logoutprofile() {
	return new SingleLogoutProfileImpl();
    }

    @Bean
    public ExtendedMetadataDelegate idpMetadata() throws MetadataProviderException, ResourceException {

	Timer backgroundTaskTimer = new Timer(true);

//	ResourceBackedMetadataProvider resourceBackedMetadataProvider = new ResourceBackedMetadataProvider(
//		backgroundTaskTimer, new ClasspathResource("federationMetadata"));

        String fedMetadataURL = "https://sts.nist.gov/federationmetadata/2007-06/federationmetadata.xml";
	HTTPMetadataProvider httpMetadataProvider = new HTTPMetadataProvider(
			backgroundTaskTimer, httpClient(), fedMetadataURL);
	httpMetadataProvider.setParserPool(parserPool());

//	resourceBackedMetadataProvider.setParserPool(parserPool());
//
//	ExtendedMetadataDelegate extendedMetadataDelegate = new ExtendedMetadataDelegate(resourceBackedMetadataProvider,
//		extendedMetadata());
        ExtendedMetadataDelegate extendedMetadataDelegate =
                new ExtendedMetadataDelegate(httpMetadataProvider , extendedMetadata());

	//// **** just set this to false to solve the issue signature trust
	//// establishment
	extendedMetadataDelegate.setMetadataTrustCheck(false);
	extendedMetadataDelegate.setMetadataRequireSignature(false);
	return extendedMetadataDelegate;
    }

    @Bean
    @Qualifier("metadata")
    public CachingMetadataManager metadata() throws MetadataProviderException, ResourceException {
	List<MetadataProvider> providers = new ArrayList<>();
	providers.add(idpMetadata());
	return new CachingMetadataManager(providers);
    }

    @Bean
    public SAMLUserDetailsService samlUserDetailsService() {
	return new SamlUserDetailsService();
    }

    @Bean
    public SAMLAuthenticationProvider samlAuthenticationProvider() {
	SAMLAuthenticationProvider samlAuthenticationProvider = new SAMLAuthenticationProvider();
	samlAuthenticationProvider.setUserDetails(samlUserDetailsService());
	samlAuthenticationProvider.setForcePrincipalAsString(false);
	return samlAuthenticationProvider;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
	auth.authenticationProvider(samlAuthenticationProvider());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

	http.addFilterBefore(corsFilter(), SessionManagementFilter.class).exceptionHandling()
		.authenticationEntryPoint(samlEntryPoint());
	http.csrf().disable();

	http.addFilterBefore(metadataGeneratorFilter(), ChannelProcessingFilter.class).addFilterAfter(samlFilter(),
		BasicAuthenticationFilter.class);

	http.authorizeRequests().antMatchers("/error").permitAll().antMatchers("/saml/**").permitAll().anyRequest()
		.authenticated();

	http.logout().logoutSuccessUrl("/");

//        http.cors();

    }

    @Bean
    CORSFilter corsFilter() {
	CORSFilter filter = new CORSFilter();
	return filter;
    }

//  private Timer backgroundTaskTimer;
//	private MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager;
//
//	public void init() {
//		this.backgroundTaskTimer = new Timer(true);
//		this.multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();
//	}
//
//	public void shutdown() {
//		this.backgroundTaskTimer.purge();
//		this.backgroundTaskTimer.cancel();
//		this.multiThreadedHttpConnectionManager.shutdown();
//	}
}