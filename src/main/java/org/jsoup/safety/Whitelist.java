package org.jsoup.safety;

/*
    Thank you to Ryan Grove (wonko.com) for the Ruby HTML cleaner http://github.com/rgrove/sanitize/, which inspired
    this whitelist configuration, and the initial defaults.
 */

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jsoup.helper.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;


/**
 Whitelists define what HTML (elements and attributes) to allow through the cleaner. Everything else is removed.
 <p>
 Start with one of the defaults:
 </p>
 <ul>
 <li>{@link #none}
 <li>{@link #simpleText}
 <li>{@link #basic}
 <li>{@link #basicWithImages}
 <li>{@link #relaxed}
 </ul>
 <p>
 If you need to allow more through (please be careful!), tweak a base whitelist with:
 </p>
 <ul>
 <li>{@link #addTags}
 <li>{@link #addAttributes}
 <li>{@link #addEnforcedAttribute}
 <li>{@link #addProtocols}
 </ul>
 <p>
 You can remove any setting from an existing whitelist with:
 </p>
 <ul>
 <li>{@link #removeTags}
 <li>{@link #removeAttributes}
 <li>{@link #removeEnforcedAttribute}
 <li>{@link #removeProtocols}
 </ul>
 
 <p>
 The cleaner and these whitelists assume that you want to clean a <code>body</code> fragment of HTML (to add user
 supplied HTML into a templated page), and not to clean a full HTML document. If the latter is the case, either wrap the
 document HTML around the cleaned body HTML, or create a whitelist that allows <code>html</code> and <code>head</code>
 elements as appropriate.
 </p>
 <p>
 If you are going to extend a whitelist, please be very careful. Make sure you understand what attributes may lead to
 XSS attack vectors. URL attributes are particularly vulnerable and require careful validation. See 
 http://ha.ckers.org/xss.html for some XSS attack examples.
 </p>

 @author Jonathan Hedley
 @author knowings
 */
public class Whitelist {
	private static final AttributeKey HREF_ATTR = AttributeKey.valueOf("href");
	private static final AttributeKey SRC_ATTR = AttributeKey.valueOf("src");
	
    private Set<TagName> tagNames; // tags allowed, lower case. e.g. [p, br, span]
    private Map<TagName, Set<AttributeKey>> attributes; // tag -> attribute[]. allowed attributes [href] for a tag.
    private Map<TagName, Map<AttributeKey, AttributeValue>> enforcedAttributes; // always set these attribute values
    private Map<TagName, Map<AttributeKey, Set<Protocol>>> protocols; // allowed URL protocols for attributes
    private Map<TagName,Map<AttributeKey, Set<UrlDomain>>> domains; // allowed URL domains for attributes
    private boolean preserveRelativeLinks; // option to preserve relative links

    /**
     This whitelist allows only text nodes: all HTML will be stripped.

     @return whitelist
     */
    public static Whitelist none() {
        return new Whitelist();
    }

    /**
     This whitelist allows only simple text formatting: <code>b, em, i, strong, u</code>. All other HTML (tags and
     attributes) will be removed.

     @return whitelist
     */
    public static Whitelist simpleText() {
        return new Whitelist()
                .addTags("b", "em", "i", "strong", "u")
                ;
    }

    /**
     <p>
     This whitelist allows a fuller range of text nodes: <code>a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li,
     ol, p, pre, q, small, span, strike, strong, sub, sup, u, ul</code>, and appropriate attributes.
     </p>
     <p>
     Links (<code>a</code> elements) can point to <code>http, https, ftp, mailto</code>, and have an enforced
     <code>rel=nofollow</code> attribute.
     </p>
     <p>
     Does not allow images.
     </p>

     @return whitelist
     */
    public static Whitelist basic() {
        return new Whitelist()
                .addTags(
                        "a", "b", "blockquote", "br", "cite", "code", "dd", "dl", "dt", "em",
                        "i", "li", "ol", "p", "pre", "q", "small", "span", "strike", "strong", "sub",
                        "sup", "u", "ul")

                .addAttributes("a", "href")
                .addAttributes("blockquote", "cite")
                .addAttributes("q", "cite")

                .addProtocols("a", "href", "ftp", "http", "https", "mailto")
                .addProtocols("blockquote", "cite", "http", "https")
                .addProtocols("cite", "cite", "http", "https")

                .addEnforcedAttribute("a", "rel", "nofollow")
                ;

    }

    /**
     This whitelist allows the same text tags as {@link #basic}, and also allows <code>img</code> tags, with appropriate
     attributes, with <code>src</code> pointing to <code>http</code> or <code>https</code>.

     @return whitelist
     */
    public static Whitelist basicWithImages() {
        return basic()
                .addTags("img")
                .addAttributes("img", "align", "alt", "height", "src", "title", "width")
                .addProtocols("img", "src", "http", "https")
                ;
    }

    /**
     This whitelist allows a full range of text and structural body HTML: <code>a, b, blockquote, br, caption, cite,
     code, col, colgroup, dd, div, dl, dt, em, h1, h2, h3, h4, h5, h6, i, img, li, ol, p, pre, q, small, span, strike, strong, sub,
     sup, table, tbody, td, tfoot, th, thead, tr, u, ul</code>
     <p>
     Links do not have an enforced <code>rel=nofollow</code> attribute, but you can add that if desired.
     </p>

     @return whitelist
     */
    public static Whitelist relaxed() {
        return new Whitelist()
                .addTags(
                        "a", "b", "blockquote", "br", "caption", "cite", "code", "col",
                        "colgroup", "dd", "div", "dl", "dt", "em", "h1", "h2", "h3", "h4", "h5", "h6",
                        "i", "img", "li", "ol", "p", "pre", "q", "small", "span", "strike", "strong",
                        "sub", "sup", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "u",
                        "ul")

                .addAttributes("a", "href", "title")
                .addAttributes("blockquote", "cite")
                .addAttributes("col", "span", "width")
                .addAttributes("colgroup", "span", "width")
                .addAttributes("img", "align", "alt", "height", "src", "title", "width")
                .addAttributes("ol", "start", "type")
                .addAttributes("q", "cite")
                .addAttributes("table", "summary", "width")
                .addAttributes("td", "abbr", "axis", "colspan", "rowspan", "width")
                .addAttributes(
                        "th", "abbr", "axis", "colspan", "rowspan", "scope",
                        "width")
                .addAttributes("ul", "type")

                .addProtocols("a", "href", "ftp", "http", "https", "mailto")
                .addProtocols("blockquote", "cite", "http", "https")
                .addProtocols("cite", "cite", "http", "https")
                .addProtocols("img", "src", "http", "https")
                .addProtocols("q", "cite", "http", "https")
                ;
    }

    /**
     Create a new, empty whitelist. Generally it will be better to start with a default prepared whitelist instead.

     @see #basic()
     @see #basicWithImages()
     @see #simpleText()
     @see #relaxed()
     */
    public Whitelist() {
        tagNames = new HashSet<TagName>();
        attributes = new HashMap<TagName, Set<AttributeKey>>();
        enforcedAttributes = new HashMap<TagName, Map<AttributeKey, AttributeValue>>();
        protocols = new HashMap<TagName, Map<AttributeKey, Set<Protocol>>>();
        domains = new HashMap<TagName,Map<AttributeKey, Set<UrlDomain>>>();
        preserveRelativeLinks = false;
    }

    /**
     Add a list of allowed elements to a whitelist. (If a tag is not allowed, it will be removed from the HTML.)

     @param tags tag names to allow
     @return this (for chaining)
     */
    public Whitelist addTags(String... tags) {
        Validate.notNull(tags);

        for (String tagName : tags) {
            Validate.notEmpty(tagName);
            tagNames.add(TagName.valueOf(tagName));
        }
        return this;
    }

    /**
     Remove a list of allowed elements from a whitelist. (If a tag is not allowed, it will be removed from the HTML.)

     @param tags tag names to disallow
     @return this (for chaining)
     */
    public Whitelist removeTags(String... tags) {
        Validate.notNull(tags);

        for(String tag: tags) {
            Validate.notEmpty(tag);
            TagName tagName = TagName.valueOf(tag);

            if(tagNames.remove(tagName)) { // Only look in sub-maps if tag was allowed
                attributes.remove(tagName);
                enforcedAttributes.remove(tagName);
                protocols.remove(tagName);
            }
        }
        return this;
    }

    /**
     Add a list of allowed attributes to a tag. (If an attribute is not allowed on an element, it will be removed.)
     <p>
     E.g.: <code>addAttributes("a", "href", "class")</code> allows <code>href</code> and <code>class</code> attributes
     on <code>a</code> tags.
     </p>
     <p>
     To make an attribute valid for <b>all tags</b>, use the pseudo tag <code>:all</code>, e.g.
     <code>addAttributes(":all", "class")</code>.
     </p>

     @param tag  The tag the attributes are for. The tag will be added to the allowed tag list if necessary.
     @param keys List of valid attributes for the tag
     @return this (for chaining)
     */
    public Whitelist addAttributes(String tag, String... keys) {
        Validate.notEmpty(tag);
        Validate.notNull(keys);
        Validate.isTrue(keys.length > 0, "No attributes supplied.");

        TagName tagName = TagName.valueOf(tag);
        if (!tagNames.contains(tagName))
            tagNames.add(tagName);
        Set<AttributeKey> attributeSet = new HashSet<AttributeKey>();
        for (String key : keys) {
            Validate.notEmpty(key);
            attributeSet.add(AttributeKey.valueOf(key));
        }
        if (attributes.containsKey(tagName)) {
            Set<AttributeKey> currentSet = attributes.get(tagName);
            currentSet.addAll(attributeSet);
        } else {
            attributes.put(tagName, attributeSet);
        }
        return this;
    }

    /**
     Remove a list of allowed attributes from a tag. (If an attribute is not allowed on an element, it will be removed.)
     <p>
     E.g.: <code>removeAttributes("a", "href", "class")</code> disallows <code>href</code> and <code>class</code>
     attributes on <code>a</code> tags.
     </p>
     <p>
     To make an attribute invalid for <b>all tags</b>, use the pseudo tag <code>:all</code>, e.g.
     <code>removeAttributes(":all", "class")</code>.
     </p>

     @param tag  The tag the attributes are for.
     @param keys List of invalid attributes for the tag
     @return this (for chaining)
     */
    public Whitelist removeAttributes(String tag, String... keys) {
        Validate.notEmpty(tag);
        Validate.notNull(keys);
        Validate.isTrue(keys.length > 0, "No attributes supplied.");

        TagName tagName = TagName.valueOf(tag);
        Set<AttributeKey> attributeSet = new HashSet<AttributeKey>();
        for (String key : keys) {
            Validate.notEmpty(key);
            attributeSet.add(AttributeKey.valueOf(key));
        }
        if(tagNames.contains(tagName) && attributes.containsKey(tagName)) { // Only look in sub-maps if tag was allowed
            Set<AttributeKey> currentSet = attributes.get(tagName);
            currentSet.removeAll(attributeSet);

            if(currentSet.isEmpty()) // Remove tag from attribute map if no attributes are allowed for tag
                attributes.remove(tagName);
        }
        if(tag.equals(":all")) // Attribute needs to be removed from all individually set tags
            for(TagName name: attributes.keySet()) {
                Set<AttributeKey> currentSet = attributes.get(name);
                currentSet.removeAll(attributeSet);

                if(currentSet.isEmpty()) // Remove tag from attribute map if no attributes are allowed for tag
                    attributes.remove(name);
            }
        return this;
    }

    /**
     Add an enforced attribute to a tag. An enforced attribute will always be added to the element. If the element
     already has the attribute set, it will be overridden.
     <p>
     E.g.: <code>addEnforcedAttribute("a", "rel", "nofollow")</code> will make all <code>a</code> tags output as
     <code>&lt;a href="..." rel="nofollow"&gt;</code>
     </p>

     @param tag   The tag the enforced attribute is for. The tag will be added to the allowed tag list if necessary.
     @param key   The attribute key
     @param value The enforced attribute value
     @return this (for chaining)
     */
    public Whitelist addEnforcedAttribute(String tag, String key, String value) {
        Validate.notEmpty(tag);
        Validate.notEmpty(key);
        Validate.notEmpty(value);

        TagName tagName = TagName.valueOf(tag);
        if (!tagNames.contains(tagName))
            tagNames.add(tagName);
        AttributeKey attrKey = AttributeKey.valueOf(key);
        AttributeValue attrVal = AttributeValue.valueOf(value);

        if (enforcedAttributes.containsKey(tagName)) {
            enforcedAttributes.get(tagName).put(attrKey, attrVal);
        } else {
            Map<AttributeKey, AttributeValue> attrMap = new HashMap<AttributeKey, AttributeValue>();
            attrMap.put(attrKey, attrVal);
            enforcedAttributes.put(tagName, attrMap);
        }
        return this;
    }

    /**
     Remove a previously configured enforced attribute from a tag.

     @param tag   The tag the enforced attribute is for.
     @param key   The attribute key
     @return this (for chaining)
     */
    public Whitelist removeEnforcedAttribute(String tag, String key) {
        Validate.notEmpty(tag);
        Validate.notEmpty(key);

        TagName tagName = TagName.valueOf(tag);
        if(tagNames.contains(tagName) && enforcedAttributes.containsKey(tagName)) {
            AttributeKey attrKey = AttributeKey.valueOf(key);
            Map<AttributeKey, AttributeValue> attrMap = enforcedAttributes.get(tagName);
            attrMap.remove(attrKey);

            if(attrMap.isEmpty()) // Remove tag from enforced attribute map if no enforced attributes are present
                enforcedAttributes.remove(tagName);
        }
        return this;
    }

    /**
     * Configure this Whitelist to preserve relative links in an element's URL attribute, or convert them to absolute
     * links. By default, this is <b>false</b>: URLs will be  made absolute (e.g. start with an allowed protocol, like
     * e.g. {@code http://}.
     * <p>
     * Note that when handling relative links, the input document must have an appropriate {@code base URI} set when
     * parsing, so that the link's protocol can be confirmed. Regardless of the setting of the {@code preserve relative
     * links} option, the link must be resolvable against the base URI to an allowed protocol; otherwise the attribute
     * will be removed.
     * </p>
     *
     * @param preserve {@code true} to allow relative links, {@code false} (default) to deny
     * @return this Whitelist, for chaining.
     * @see #addProtocols
     */
    public Whitelist preserveRelativeLinks(boolean preserve) {
        preserveRelativeLinks = preserve;
        return this;
    }

    /**
     Add allowed URL protocols for an element's URL attribute. This restricts the possible values of the attribute to
     URLs with the defined protocol.
     <p>
     E.g.: <code>addProtocols("a", "href", "ftp", "http", "https")</code>
     </p>
     <p>
     To allow a link to an in-page URL anchor (i.e. <code>&lt;a href="#anchor"&gt;</code>, add a <code>#</code>:<br>
     E.g.: <code>addProtocols("a", "href", "#")</code>
     </p>

     @param tag       Tag the URL protocol is for
     @param key       Attribute key
     @param protocols List of valid protocols
     @return this, for chaining
     */
    public Whitelist addProtocols(String tag, String key, String... protocols) {
        Validate.notEmpty(tag);
        Validate.notEmpty(key);
        Validate.notNull(protocols);

        TagName tagName = TagName.valueOf(tag);
        AttributeKey attrKey = AttributeKey.valueOf(key);
        Map<AttributeKey, Set<Protocol>> attrMap;
        Set<Protocol> protSet;

        if (this.protocols.containsKey(tagName)) {
            attrMap = this.protocols.get(tagName);
        } else {
            attrMap = new HashMap<AttributeKey, Set<Protocol>>();
            this.protocols.put(tagName, attrMap);
        }
        if (attrMap.containsKey(attrKey)) {
            protSet = attrMap.get(attrKey);
        } else {
            protSet = new HashSet<Protocol>();
            attrMap.put(attrKey, protSet);
        }
        for (String protocol : protocols) {
            Validate.notEmpty(protocol);
            Protocol prot = Protocol.valueOf(protocol);
            protSet.add(prot);
        }
        return this;
    }

    /**
    Add allowed URL domain for an element's URL attribute. This restricts the possible values of the attribute to
    URLs with the defined protocol.
    <p>
    E.g.: <code>addDomains("a", "href", "jsoup.org")</code>
    </p>

    @param tag       Tag the allowed domain is for
    @param key       Attribute key
    @param domains List of valid domains
    @return this, for chaining
    */
    public Whitelist addDomains(String tag, String key, String... domains) {
    	Validate.notEmpty(tag);
    	Validate.notEmpty(key);
    	Validate.notNull(domains);

    	TagName tagName = TagName.valueOf(tag);
    	AttributeKey attrKey = AttributeKey.valueOf(key);
    	Map<AttributeKey, Set<UrlDomain>> attrMap;
    	Set<UrlDomain> domainSet;

    	if (this.domains.containsKey(tagName)) {
    		attrMap = this.domains.get(tagName);
    	} else {
    		attrMap = new HashMap<AttributeKey, Set<UrlDomain>>();
    		this.domains.put(tagName, attrMap);
    	}
    	if (attrMap.containsKey(attrKey)) {
    		domainSet = attrMap.get(attrKey);
    	} else {
    		domainSet = new HashSet<UrlDomain>();
    		attrMap.put(attrKey, domainSet);
    	}
    	for (String domain : domains) {
    		Validate.notEmpty(domain);
    		UrlDomain urlDomain = UrlDomain.valueOf(domain);
    		domainSet.add(urlDomain);
    	}
    	return this;
   	}
    
    /**
     Remove allowed URL protocols for an element's URL attribute.
     <p>
     E.g.: <code>removeProtocols("a", "href", "ftp")</code>
     </p>

     @param tag       Tag the URL protocol is for
     @param key       Attribute key
     @param protocols List of invalid protocols
     @return this, for chaining
     */
    public Whitelist removeProtocols(String tag, String key, String... protocols) {
        Validate.notEmpty(tag);
        Validate.notEmpty(key);
        Validate.notNull(protocols);

        TagName tagName = TagName.valueOf(tag);
        AttributeKey attrKey = AttributeKey.valueOf(key);

        if(this.protocols.containsKey(tagName)) {
            Map<AttributeKey, Set<Protocol>> attrMap = this.protocols.get(tagName);
            if(attrMap.containsKey(attrKey)) {
                Set<Protocol> protSet = attrMap.get(attrKey);
                for (String protocol : protocols) {
                    Validate.notEmpty(protocol);
                    Protocol prot = Protocol.valueOf(protocol);
                    protSet.remove(prot);
                }

                if(protSet.isEmpty()) { // Remove protocol set if empty
                    attrMap.remove(attrKey);
                    if(attrMap.isEmpty()) // Remove entry for tag if empty
                        this.protocols.remove(tagName);
                }
            }
        }
        return this;
    }
    
    /**
     Remove allowed domains for an element's URL attribute.
     <p>
     E.g.: <code>removeDomains("a", "href", "knowings.fr")</code>
     </p>

     @param tag       Tag the URL protocol is for
     @param key       Attribute key
     @param domains List of invalid protocols
     @return this, for chaining
     */
    public Whitelist removeDomains(String tag, String key, String... domains) {
    	Validate.notEmpty(tag);
    	Validate.notEmpty(key);
    	Validate.notNull(domains);
    	
    	TagName tagName = TagName.valueOf(tag);
    	AttributeKey attrKey = AttributeKey.valueOf(key);
    	
    	if(this.domains.containsKey(tagName)) {
    		Map<AttributeKey, Set<UrlDomain>> attrMap = this.domains.get(tagName);
    		if(attrMap.containsKey(attrKey)) {
    			Set<UrlDomain> domainSet = attrMap.get(attrKey);
    			for (String urlDomain : domains) {
    				Validate.notEmpty(urlDomain);
    				UrlDomain domain = UrlDomain.valueOf(urlDomain);
    				domainSet.remove(domain);
    			}
    			
    			if(domainSet.isEmpty()) { // Remove protocol set if empty
    				attrMap.remove(attrKey);
    				if(attrMap.isEmpty()) // Remove entry for tag if empty
    					this.domains.remove(tagName);
    			}
    		}
    	}
    	return this;
    }

    /**
     * Test if the supplied tag is allowed by this whitelist
     * @param tag test tag
     * @return true if allowed
     */
    protected boolean isSafeTag(String tag) {
        return tagNames.contains(TagName.valueOf(tag));
    }
    
    /**
     * Test if the supplied tag is allowed by this whitelist
     * @param tagElement test tag
     * @return true if allowed
     */
    protected boolean isSafeTag(Element tagElement) {
    	boolean isSafe = false;
    	if (tagElement != null) {
    		TagName tag = TagName.valueOf(tagElement.tagName());
			if (tagNames.contains(tag)) {
				isSafe = true;
				if (domains.containsKey(tag)) {
					Map<AttributeKey,Set<UrlDomain>> attrDomains = domains.get(tag);
					if (tagElement.hasAttr(HREF_ATTR.toString()) && attrDomains.containsKey(HREF_ATTR)) {
						isSafe = testValidDomain(tagElement, HREF_ATTR, attrDomains.get(HREF_ATTR));
					}
					else if (tagElement.hasAttr(SRC_ATTR.toString()) && attrDomains.containsKey(SRC_ATTR)) {
						isSafe = testValidDomain(tagElement, SRC_ATTR, attrDomains.get(SRC_ATTR));
					}
				}
			}
		}
    	return isSafe;
    }

    /**
     * Test if the supplied attribute is allowed by this whitelist for this tag
     * @param tagName tag to consider allowing the attribute in
     * @param el element under test, to confirm protocol
     * @param attr attribute under test
     * @return true if allowed
     */
    protected boolean isSafeAttribute(String tagName, Element el, Attribute attr) {
        TagName tag = TagName.valueOf(tagName);
        AttributeKey key = AttributeKey.valueOf(attr.getKey());

        if (attributes.containsKey(tag)) {
            if (attributes.get(tag).contains(key)) {
                if (protocols.containsKey(tag)) {
                    Map<AttributeKey, Set<Protocol>> attrProts = protocols.get(tag);
                    // ok if not defined protocol; otherwise test
                    return !attrProts.containsKey(key) || testValidProtocol(el, attr, attrProts.get(key));
                } else { // attribute found, no protocols defined, so OK
                    return true;
                }
            }
        }
        // no attributes defined for tag, try :all tag
        return !tagName.equals(":all") && isSafeAttribute(":all", el, attr);
    }

    /**
     * Tells whether or not the given tag is already configured.
     * @param tagName the tag to consider. When null or empty false is returned.
     * @return true or false.
     */
    public boolean isTagConfigured(String tagName) {
    	boolean isConfigured = false;
    	if (!StringUtil.isBlank(tagName)) {
    		isConfigured = tagNames.contains(TagName.valueOf(tagName));
    	}
    	return isConfigured;
    }
    
    /**
     * Tells whether or not the given attribute is already configured for the given tag.
     * @param tagName the tag to consider. When null or empty false is returned.
     * @param attributeKey the attribute to consider. When null or empty false is returned.
     * @return true or false.
     */
    public boolean isAttributeConfigured(String tagName, String attributeKey) {
    	boolean isConfigured = false;
    	if (!StringUtil.isBlank(tagName) && !StringUtil.isBlank(attributeKey)) {
    		TagName tag = TagName.valueOf(tagName);
    		AttributeKey attr = AttributeKey.valueOf(attributeKey);
    		isConfigured = tagNames.contains(tag) && attributes.containsKey(tag) && attributes.get(tag).contains(attr);
    	}
    	return isConfigured;
    }
    
    /**
     * Tells whether or not the given protocol is already configured for the given tag on the given attribute.
     * @param tagName the tag to consider. When null or empty false is returned.
     * @param attributeKey the attribute to consider. When null or empty false is returned.
     * @param protocol the protocol to consider. When null or empty false is returned.
     * @return true or false
     */
    public boolean isProtocolConfigured(String tagName, String attributeKey, String protocol) {
    	boolean isConfigured = false;
    	if (!StringUtil.isBlank(tagName) && !StringUtil.isBlank(attributeKey) && !StringUtil.isBlank(protocol)) {
    		TagName tag = TagName.valueOf(tagName);
    		AttributeKey attr = AttributeKey.valueOf(attributeKey);
    		Protocol prot = Protocol.valueOf(protocol);
    		isConfigured = protocols.containsKey(tag) && protocols.get(tag).containsKey(attr) && protocols.get(tag).get(attr).contains(prot);
    	}
    	return isConfigured;
    }
    
    public boolean isDomainConfigured(String tagName, String attributeKey, String domain) {
    	boolean isConfigured = false;
    	if (!StringUtil.isBlank(tagName) && !StringUtil.isBlank(attributeKey) && !StringUtil.isBlank(domain)) {
    		TagName tag = TagName.valueOf(tagName);
    		AttributeKey attr = AttributeKey.valueOf(attributeKey);
    		UrlDomain dom = UrlDomain.valueOf(domain);
    		isConfigured = domains.containsKey(tag) && domains.get(tag).containsKey(attr) && domains.get(tag).get(attr).contains(dom);
    	}
    	return isConfigured;
    }
    
    private boolean testValidProtocol(Element el, Attribute attr, Set<Protocol> protocols) {
        // try to resolve relative urls to abs, and optionally update the attribute so output html has abs.
        // rels without a baseuri get removed
        String value = el.absUrl(attr.getKey());
        if (value.length() == 0)
            value = attr.getValue(); // if it could not be made abs, run as-is to allow custom unknown protocols
        if (!preserveRelativeLinks)
            attr.setValue(value);
        
        for (Protocol protocol : protocols) {
            String prot = protocol.toString();

            if (prot.equals("#")) { // allows anchor links
                if (isValidAnchor(value)) {
                    return true;
                } else {
                    continue;
                }
            }

            prot += ":";

            if (value.toLowerCase().startsWith(prot)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean testValidDomain(Element el, AttributeKey attrKey, Set<UrlDomain> domains) {
    	if (domains == null || domains.isEmpty()) {
    		return true;
    	}
    	String _url = el.attr(attrKey.toString());
    	if (StringUtil.isBlank(_url)) {
    		return false;
    	}
    	if (_url.startsWith("#")) {
    		return isValidAnchor(_url);
    	}
    	_url = el.absUrl(attrKey.toString());
    	if (_url.length() == 0) {
    		_url = el.attr(attrKey.toString());
    		_url = ((_url.startsWith("//"))? "http:" : "http://"  ) + _url;
    	}
    	try {
    		URL url = new URL(_url);
    		String host = url.getHost();
    		if (!StringUtil.isBlank(host)) {
    			host = host.toLowerCase();
    			Iterator<UrlDomain> it = domains.iterator();
    			boolean isValid = false;
    			while (it.hasNext() && !isValid) {
    				String domain = it.next().toString().toLowerCase();
    				isValid = host.equals(domain) || host.endsWith("."+domain); 
    			}
    			return isValid;
    		}
    	} catch(MalformedURLException ex) {
    		// Can't get enough intel on the supplied url, it should return false.
    	}
    	return false;
    }
    
    private boolean isValidAnchor(String value) {
        return value.startsWith("#") && !value.matches(".*\\s.*");
    }

    Attributes getEnforcedAttributes(String tagName) {
        Attributes attrs = new Attributes();
        TagName tag = TagName.valueOf(tagName);
        if (enforcedAttributes.containsKey(tag)) {
            Map<AttributeKey, AttributeValue> keyVals = enforcedAttributes.get(tag);
            for (Map.Entry<AttributeKey, AttributeValue> entry : keyVals.entrySet()) {
                attrs.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        return attrs;
    }
    
    // named types for config. All just hold strings, but here for my sanity.

    static class TagName extends TypedValue {
        TagName(String value) {
            super(value);
        }

        static TagName valueOf(String value) {
            return new TagName(value);
        }
    }

    static class AttributeKey extends TypedValue {
        AttributeKey(String value) {
            super(value);
        }

        static AttributeKey valueOf(String value) {
            return new AttributeKey(value);
        }
    }

    static class AttributeValue extends TypedValue {
        AttributeValue(String value) {
            super(value);
        }

        static AttributeValue valueOf(String value) {
            return new AttributeValue(value);
        }
    }

    static class Protocol extends TypedValue {
        Protocol(String value) {
            super(value);
        }

        static Protocol valueOf(String value) {
            return new Protocol(value);
        }
    }
    
    static class UrlDomain extends TypedValue {
    	UrlDomain(String value) {
    		super(value);
    	}
    	
    	static UrlDomain valueOf(String value) {
    		return new UrlDomain(value);
    	}
    }

    abstract static class TypedValue {
        private String value;

        TypedValue(String value) {
            Validate.notNull(value);
            this.value = value;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            TypedValue other = (TypedValue) obj;
            if (value == null) {
                if (other.value != null) return false;
            } else if (!value.equals(other.value)) return false;
            return true;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}

