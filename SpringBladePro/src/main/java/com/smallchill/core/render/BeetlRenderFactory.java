/*
[The "BSD license"]
Copyright (c) 2011-2013 Joel Li (李家智)
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.smallchill.core.render;

import java.io.IOException;

import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.resource.WebAppResourceLoader;

import com.smallchill.common.beetl.BeetlRegister;
import com.smallchill.core.modules.beetl.BeetlTemplate;
import com.smallchill.core.toolbox.kit.PathKit;

public class BeetlRenderFactory {
	public static String viewExtension = ".html";
	public static GroupTemplate groupTemplate = null;

	public BeetlRenderFactory() {
		init(PathKit.getWebRootPath());
	}

	private void init(String root) {
		if (groupTemplate != null) {
			groupTemplate.close();
		}
		try {
			Configuration cfg = Configuration.defaultConfiguration();
			WebAppResourceLoader resourceLoader = new WebAppResourceLoader(root);
			groupTemplate = new GroupTemplate(resourceLoader, cfg);
			BeetlTemplate.registerTemplate(groupTemplate);
			BeetlRegister.registerTemplate(groupTemplate);
		} catch (IOException e) {
			throw new RuntimeException("加载GroupTemplate失败", e);
		}
	}

	public Render getRender(String view) {
		return new BeetlRender(groupTemplate, view);
	}

	public String getViewExtension() {
		return viewExtension;
	}
}
