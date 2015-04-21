/**
 * 
 */
package org.jsoup.safety;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Test for the whitelist.
 * 
 * @author developers@knowings.com
 */
public class WhitelistTest {

	/**
	 * Test method for {@link org.jsoup.safety.Whitelist#isTagConfigured(java.lang.String)}.
	 */
	@Test
	public void testIsTagConfigured() {
		Whitelist whitelist = Whitelist.none();
		assertFalse(whitelist.isTagConfigured("a"));
		// Once added, it should be considered as configured.
		whitelist.addTags("a");
		assertTrue(whitelist.isTagConfigured("a"));
		// Adding an attribute automatically configure the holding tag.
		assertFalse(whitelist.isTagConfigured("img"));
		whitelist.addAttributes("img", "src");
		assertTrue(whitelist.isTagConfigured("img"));
		// Check pre-configured whitelists behave correctly.
		assertFalse(Whitelist.simpleText().isTagConfigured("a"));
		assertTrue(Whitelist.basic().isTagConfigured("a"));
	}

	/**
	 * Test method for {@link org.jsoup.safety.Whitelist#isAttributeConfigured(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testIsAttributeConfigured() {
		Whitelist whitelist = Whitelist.none();
		assertFalse(whitelist.isAttributeConfigured("a", "href"));
		// Once added, it should be considered as configured.
		whitelist.addAttributes("a", "href");
		assertTrue(whitelist.isAttributeConfigured("a", "href"));
		// Check pre-configured whitelists behave correctly.
		assertTrue(Whitelist.basic().isAttributeConfigured("a","href"));
	}

	/**
	 * Test method for {@link org.jsoup.safety.Whitelist#isProtocolConfigured(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testIsProtocolConfigured() {
		Whitelist whitelist = Whitelist.none();
		assertFalse(whitelist.isProtocolConfigured("a", "href", "http"));
		// Once added, it should be considered as configured.
		whitelist.addProtocols("a", "href", "http");
		assertTrue(whitelist.isProtocolConfigured("a", "href", "http"));
		// Check pre-configured whitelists behave correctly.
		assertTrue(Whitelist.basic().isProtocolConfigured("a","href", "ftp"));
	}

	/**
	 * Test method for {@link org.jsoup.safety.Whitelist#isDomainConfigured(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testIsDomainConfigured() {
		Whitelist whitelist = Whitelist.none();
		assertFalse(whitelist.isDomainConfigured("a", "href", "knowings.fr"));
		// Once added, it should be considered as configured.
		whitelist.addDomains("a", "href", "knowings.fr");
		assertTrue(whitelist.isDomainConfigured("a", "href", "knowings.fr"));
		// Check pre-configured whitelists behave correctly.
		assertFalse(Whitelist.basic().isDomainConfigured("a","href", "knowings.fr"));
	}

}
