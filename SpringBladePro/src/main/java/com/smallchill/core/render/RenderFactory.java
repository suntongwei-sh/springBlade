/**
 * Copyright (c) 2011-2016, James Zhan 詹波 (jfinal@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smallchill.core.render;

import java.io.File;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * RenderFactory.
 */
public class RenderFactory {
	
	private static final RenderFactory me = new RenderFactory();
	
	private BeetlRenderFactory mainRenderFactory = new BeetlRenderFactory();
	
	private RenderFactory() {
		
	}
	
	public static RenderFactory me() {
		return me;
	}
	
	public Render getBeetlRender(String view) {
		return mainRenderFactory.getRender(view);
	}
	
	public Render getJsonRender() {
		return new JsonRender();
	}
	
	public Render getJsonRender(String key, Object value) {
		return new JsonRender(key, value);
	}
	
	public Render getJsonRender(String[] attrs) {
		return new JsonRender(attrs);
	}
	
	public Render getJsonRender(String jsonText) {
		return new JsonRender(jsonText);
	}
	
	public Render getJsonRender(Object object) {
		return new JsonRender(object);
	}
	
	public Render getTextRender(String text) {
		return new TextRender(text);
	}
	
	public Render getTextRender(String text, String contentType) {
		return new TextRender(text, contentType);
	}
	
	public Render getTextRender(String text, ContentType contentType) {
		return new TextRender(text, contentType);
	}
	
	public Render getFileRender(File file) {
		return new FileRender(file);
	}
	
	public Render getJavascriptRender(String jsText) {
		return new JavascriptRender(jsText);
	}
	
	public Render getHtmlRender(String htmlText) {
		return new HtmlRender(htmlText);
	}
	
	public Render getCaptchaRender() {
		return new CaptchaRender();
	}
	
	public Render getRedirectRender(String url) {
		return new RedirectRender(url);
	}
	
	public Render getRedirectRender(String url, boolean withQueryString) {
		return new RedirectRender(url, withQueryString);
	}
	
	public Render getExcelRender(HSSFWorkbook workbook, String fileName) {
		return new ExcelRender(workbook, fileName);
	}
	

}


