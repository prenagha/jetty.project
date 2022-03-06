//
// ========================================================================
// Copyright (c) 1995-2022 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.rewrite.handler;

import java.io.IOException;
import java.util.regex.Matcher;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.annotation.Name;

/**
 * <p>Issues a (3xx) redirect response whenever the rule finds a request path regular expression match</p>
 * <p>The replacement string may use {@code $n} to replace the nth capture group.</p>
 * <p>All redirects are part of the <a href="http://tools.ietf.org/html/rfc7231#section-6.4">{@code 3xx Redirection} status code set</a>.</p>
 * <p>Defaults to <a href="http://tools.ietf.org/html/rfc7231#section-6.4.3">{@code 302 Found}</a>.</p>
 */
public class RedirectRegexRule extends RegexRule
{
    protected String _location;
    private int _statusCode = HttpStatus.FOUND_302;

    public RedirectRegexRule(@Name("regex") String regex, @Name("location") String location)
    {
        super(regex);
        _location = location;
    }

    @Override
    public boolean isTerminating()
    {
        return true;
    }

    public String getLocation()
    {
        return _location;
    }

    /**
     * @param location the location to redirect.
     */
    public void setLocation(String location)
    {
        _location = location;
    }

    public int getStatusCode()
    {
        return _statusCode;
    }

    public void setStatusCode(int statusCode)
    {
        if (!HttpStatus.isRedirection(statusCode))
            throw new IllegalArgumentException("Invalid redirect status code " + statusCode + " (must be a value between 300 and 399)");
        _statusCode = statusCode;
    }

    @Override
    protected Request.WrapperProcessor apply(Request.WrapperProcessor input, Matcher matcher) throws IOException
    {
        return new Request.WrapperProcessor(input)
        {
            @Override
            public void process(Request ignored, Response response, Callback callback)
            {
                String target = getLocation();
                for (int g = 1; g <= matcher.groupCount(); ++g)
                {
                    String group = matcher.group(g);
                    target = StringUtil.replace(target, "$" + g, group);
                }

                response.setStatus(_statusCode);
                response.setHeader(HttpHeader.LOCATION, Request.toRedirectURI(this, target));
                callback.succeeded();
            }
        };
    }

    /**
     * Returns the redirect status code and replacement.
     */
    @Override
    public String toString()
    {
        return "%s[redirect:%d>%s]".formatted(super.toString(), getStatusCode(), getLocation());
    }
}
