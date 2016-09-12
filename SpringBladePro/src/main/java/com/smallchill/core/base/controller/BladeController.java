/**
 * Copyright (c) 2011-2015, James Zhan 詹波 (jfinal@126.com).
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
package com.smallchill.core.base.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.smallchill.common.vo.ShiroUser;
import com.smallchill.core.constant.Const;
import com.smallchill.core.constant.ConstCache;
import com.smallchill.core.constant.ConstCurd;
import com.smallchill.core.constant.ConstShiro;
import com.smallchill.core.constant.Cst;
import com.smallchill.core.exception.NoPermissionException;
import com.smallchill.core.exception.NoUserException;
import com.smallchill.core.interfaces.IQuery;
import com.smallchill.core.modules.support.BeanInjector;
import com.smallchill.core.render.CaptchaRender;
import com.smallchill.core.render.JsonRender;
import com.smallchill.core.render.Render;
import com.smallchill.core.render.RenderFactory;
import com.smallchill.core.shiro.ShiroKit;
import com.smallchill.core.toolbox.Func;
import com.smallchill.core.toolbox.Record;
import com.smallchill.core.toolbox.ajax.AjaxResult;
import com.smallchill.core.toolbox.file.BladeFile;
import com.smallchill.core.toolbox.grid.GridManager;
import com.smallchill.core.toolbox.kit.BeanKit;
import com.smallchill.core.toolbox.kit.LogKit;
import com.smallchill.core.toolbox.kit.PathKit;
import com.smallchill.core.toolbox.kit.StrKit;
import com.smallchill.core.toolbox.log.LogManager;

/**
 * @author James Zhan, Chill Zhuang
 */
public class BladeController implements ConstCurd, ConstCache{
	private static final Logger log = LoggerFactory.getLogger(BladeController.class);
	
	@Deprecated
	protected AjaxResult result = new AjaxResult();

	@Resource
	private HttpServletRequest request;
	
	@Resource
	private HttpServletResponse response;

	@ResponseBody
	@ExceptionHandler(Exception.class)
	public Object exceptionHandler(Exception ex, HttpServletResponse response, HttpServletRequest request) throws IOException {
		AjaxResult result = new AjaxResult();
		String url = Const.error500Path;
		String msg = ex.getMessage();
		Object resultModel = null;
		try {
			if (ex.getClass() == HttpRequestMethodNotSupportedException.class) {
				url = Const.error500Path;// 请求方式不允许抛出的异常,后面可自定义页面
			} else if (ex.getClass() == NoPermissionException.class) {
				url = Const.noPermissionPath;// 无权限抛出的异常
				msg = ConstShiro.NO_PERMISSION;
			} else if (ex.getClass() == NoUserException.class) {
				url = Const.loginRealPath;// session过期抛出的异常
				msg = ConstShiro.NO_USER;
			}
			if (isAjax()) {
				result.addFail(msg);
				resultModel = result;
			} else {
				ModelAndView view = new ModelAndView(url);
				view.addObject("error", msg);
				view.addObject("class", ex.getClass());
				view.addObject("method", request.getRequestURI());
				resultModel = view;
			}
			try {
				if(StrKit.notBlank(msg)){
					ShiroUser user = ShiroKit.getUser();
					LogManager.doLog(user, msg, "异常日志", request, false);
				}
			} catch (Exception logex) {
				LogKit.logNothing(logex);
			}
			return resultModel;
		} catch (Exception exception) {
			log.error(exception.getMessage(), exception);
			return resultModel;
		} finally {
			log.error(msg, ex);
		}
	}
	
	public HttpServletRequest getRequest() {
		return this.request;
	}

	public boolean isAjax(){
		String header = request.getHeader("X-Requested-With");
		boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(header);
		return isAjax;
	}
	
	/** ============================     session    =================================================  */
	
	/**
	 * 获取session
	 */
	public Session getSession() {
		Subject currentUser = SecurityUtils.getSubject();  
    	Session session = currentUser.getSession();
		return session;
	}

	/**
	 * Return a Object from session.
	 * 
	 * @param key
	 *            a String specifying the key of the Object stored in session
	 */
	@SuppressWarnings("unchecked")
	public <T> T getSessionAttr(String key) {
		Session session = getSession();
		return session != null ? (T) session.getAttribute(key) : null;
	}

	/**
	 * Store Object to session.
	 * 
	 * @param key
	 *            a String specifying the key of the Object stored in session
	 * @param value
	 *            a Object specifying the value stored in session
	 */
	public BladeController setSessionAttr(String key, Object value) {
		Session session = getSession();
		session.setAttribute(key, value);
		return this;
	}

	/**
	 * Remove Object in session.
	 * 
	 * @param key
	 *            a String specifying the key of the Object stored in session
	 */
	public BladeController removeSessionAttr(String key) {
		Session session = getSession();
		if (session != null)
			session.removeAttribute(key);
		return this;
	}

	/** ============================     request    =================================================  */
	
	/**
	 * Stores an attribute in this request
	 * 
	 * @param name
	 *            a String specifying the name of the attribute
	 * @param value
	 *            the Object to be stored
	 */
	public BladeController setAttr(String name, Object value) {
		request.setAttribute(name, value);
		return this;
	}

	/**
	 * Removes an attribute from this request
	 * 
	 * @param name
	 *            a String specifying the name of the attribute to remove
	 */
	public BladeController removeAttr(String name) {
		request.removeAttribute(name);
		return this;
	}

	/**
	 * Stores attributes in this request, key of the map as attribute name and
	 * value of the map as attribute value
	 * 
	 * @param attrMap
	 *            key and value as attribute of the map to be stored
	 */
	public BladeController setAttrs(Map<String, Object> attrMap) {
		for (Map.Entry<String, Object> entry : attrMap.entrySet())
			request.setAttribute(entry.getKey(), entry.getValue());
		return this;
	}

	/**
	 * Returns the value of a request parameter as a String, or null if the
	 * parameter does not exist.
	 * <p>
	 * You should only use this method when you are sure the parameter has only
	 * one value. If the parameter might have more than one value, use
	 * getParaValues(java.lang.String).
	 * <p>
	 * If you use this method with a multivalued parameter, the value returned
	 * is equal to the first value in the array returned by getParameterValues.
	 * 
	 * @param name
	 *            a String specifying the name of the parameter
	 * @return a String representing the single value of the parameter
	 */
	public String getPara(String name) {
		return request.getParameter(name);
	}

	/**
	 * Returns the value of a request parameter as a String, or default value if
	 * the parameter does not exist.
	 * 
	 * @param name
	 *            a String specifying the name of the parameter
	 * @param defaultValue
	 *            a String value be returned when the value of parameter is null
	 * @return a String representing the single value of the parameter
	 */
	public String getPara(String name, String defaultValue) {
		String result = request.getParameter(name);
		return result != null && !"".equals(result) ? result : defaultValue;
	}

	/**
	 * Returns the values of the request parameters as a Map.
	 * 
	 * @return a Map contains all the parameters name and value
	 */
	public Map<String, String[]> getParaMap() {
		return request.getParameterMap();
	}

	/**
	 * Returns an Enumeration of String objects containing the names of the
	 * parameters contained in this request. If the request has no parameters,
	 * the method returns an empty Enumeration.
	 * 
	 * @return an Enumeration of String objects, each String containing the name
	 *         of a request parameter; or an empty Enumeration if the request
	 *         has no parameters
	 */
	public Enumeration<String> getParaNames() {
		return request.getParameterNames();
	}

	/**
	 * Returns an array of String objects containing all of the values the given
	 * request parameter has, or null if the parameter does not exist. If the
	 * parameter has a single value, the array has a length of 1.
	 * 
	 * @param name
	 *            a String containing the name of the parameter whose value is
	 *            requested
	 * @return an array of String objects containing the parameter's values
	 */
	public String[] getParaValues(String name) {
		return request.getParameterValues(name);
	}

	public String getParaToDecode(String para) {
		return Func.decodeUrl(request.getParameter(para));
	}

	private Integer toInt(String value, Integer defaultValue) {
		try {
			if (value == null || "".equals(value.trim()))
				return defaultValue;
			value = value.trim();
			if (value.startsWith("N") || value.startsWith("n"))
				return -Integer.parseInt(value.substring(1));
			return Integer.parseInt(value);
		} catch (Exception e) {
			throw new RuntimeException("Can not parse the parameter \"" + value + "\" to Integer value.");
		}
	}

	/**
	 * Returns the value of a request parameter and convert to Integer.
	 * 
	 * @param name
	 *            a String specifying the name of the parameter
	 * @return a Integer representing the single value of the parameter
	 */
	public Integer getParaToInt(String name) {
		return toInt(request.getParameter(name), null);
	}

	/**
	 * Returns the value of a request parameter and convert to Integer with a
	 * default value if it is null.
	 * 
	 * @param name
	 *            a String specifying the name of the parameter
	 * @return a Integer representing the single value of the parameter
	 */
	public Integer getParaToInt(String name, Integer defaultValue) {
		return toInt(request.getParameter(name), defaultValue);
	}

	private Long toLong(String value, Long defaultValue) {
		try {
			if (value == null || "".equals(value.trim()))
				return defaultValue;
			value = value.trim();
			if (value.startsWith("N") || value.startsWith("n"))
				return -Long.parseLong(value.substring(1));
			return Long.parseLong(value);
		} catch (Exception e) {
			throw new RuntimeException("Can not parse the parameter \"" + value + "\" to Long value.");
		}
	}

	/**
	 * Returns the value of a request parameter and convert to Long.
	 * 
	 * @param name
	 *            a String specifying the name of the parameter
	 * @return a Integer representing the single value of the parameter
	 */
	public Long getParaToLong(String name) {
		return toLong(request.getParameter(name), null);
	}

	/**
	 * Returns the value of a request parameter and convert to Long with a
	 * default value if it is null.
	 * 
	 * @param name
	 *            a String specifying the name of the parameter
	 * @return a Integer representing the single value of the parameter
	 */
	public Long getParaToLong(String name, Long defaultValue) {
		return toLong(request.getParameter(name), defaultValue);
	}

	public String getRootClassPath() {
		return PathKit.getRootClassPath();
	}

	public String getWebRootPath() {
		return PathKit.getWebRootPath();
	}

	public String getContextPath() {
		return request.getContextPath();
	}

	public String getSqlId(Class<?> clazz, String sqlId) {
		return new StringBuffer().append(clazz.getName()).append(".").append(sqlId).toString();
	}

	/** ============================     mapping    =================================================  */
	
	/**
	 * 表单值映射为javabean
	 * 
	 * @param beanClass
	 *            javabean.class
	 * @return T
	 */
	public <T> T mapping(Class<T> beanClass) {
		return paraInject(beanClass);
	}

	/**
	 * 表单值映射为javabean
	 * 
	 * @param switchMap
	 *            字段混淆Map  map.put("前端字段","数据库字段");
	 * @param beanClass
	 *            javabean.class
	 * @return T
	 */
	public <T> T mapping(Map<String, Object> switchMap, Class<T> beanClass) {
		return paraInject(beanClass, switchMap);
	}

	/**
	 * 表单值映射为javabean
	 * 
	 * @param paraPerfix
	 *            name前缀
	 * @param beanClass
	 *            javabean.class
	 * @return T
	 */
	public <T> T mapping(String paraPerfix, Class<T> beanClass) {
		return paraInject(beanClass, paraPerfix);
	}

	/**
	 * 表单值映射为javabean
	 * 
	 * @param paraPerfix
	 *            name前缀
	 * @param switchMap
	 *            字段混淆Map map.put("前端字段","数据库字段");
	 * @param beanClass
	 *            javabean.class
	 * @return T
	 */
	public <T> T mapping(String paraPerfix, Map<String, Object> switchMap, Class<T> beanClass) {
		return paraInject(beanClass, switchMap, paraPerfix);
	}

	/**
	 * 表单值映射为Maps
	 * 
	 * @return Maps
	 */
	public Record getMaps() {
		return paraMapsInject();
	}

	/**
	 * 表单值映射为Maps
	 * 
	 * @param switchMap 字段混淆Map  map.put("前端字段","数据库字段");
	 * @return Maps
	 */
	public Record getMaps(Map<String, Object> switchMap) {
		return paraMapsInject(switchMap);
	}

	/**
	 * 表单值映射为Maps
	 * 
	 * @param paraPerfix  name前缀
	 * @return Maps
	 */
	public Record getMaps(String paraPerfix) {
		return paraMapsInject(paraPerfix);
	}

	/**
	 * 表单值映射为Maps
	 * 
	 * @param paraPerfix name前缀
	 * @param switchMap 字段混淆Map  map.put("前端字段","数据库字段");
	 * @return Maps
	 */
	public Record getMaps(String paraPerfix, Map<String, Object> switchMap) {
		return paraMapsInject(switchMap, paraPerfix);
	}
	
	private <T> T paraInject(Class<T> beanClass) {
		return (T) BeanInjector.inject(beanClass, this.request, false);
	}

	private <T> T paraInject(Class<T> beanClass, Map<String, Object> switchMap) {
		return (T) BeanInjector.inject(beanClass, switchMap, this.request, false);
	}

	private <T> T paraInject(Class<T> beanClass, String paraPerfix) {
		return (T) BeanInjector.inject(beanClass, paraPerfix, this.request, false);
	}

	private <T> T paraInject(Class<T> beanClass, Map<String, Object> switchMap, String paraPerfix) {
		return (T) BeanInjector.inject(beanClass, switchMap, paraPerfix, this.request, false);
	}
	
	private Record paraMapsInject() {
		return BeanInjector.injectMaps(this.request, false);
	}

	private Record paraMapsInject(Map<String, Object> switchMap) {
		return BeanInjector.injectMaps(switchMap, this.request, false);
	}

	private Record paraMapsInject(String paraPerfix) {
		return BeanInjector.injectMaps(paraPerfix, this.request, false);
	}

	private Record paraMapsInject(Map<String, Object> switchMap, String paraPerfix) {
		return BeanInjector.injectMaps(switchMap, paraPerfix, this.request, false);
	}

	public <T> T reverse(Object model) {
		return paraReverse(null, model);
	}

	public <T> T reverse(Map<String, Object> reverseMap, Object model) {
		return paraReverse(reverseMap, model);
	}

	@SuppressWarnings("unchecked")
	private <T> T paraReverse(Map<String, Object> reverseMap, Object model) {
		return (T) BeanKit.reverse(reverseMap, model);
	}
	
	/**============================     file    =================================================  */
	
	/**
	 * 获取BladeFile封装类
	 * @param file
	 * @return
	 */
	public BladeFile getFile(MultipartFile file){
		return getFile(file, null);
	}
	
	/**
	 * 获取BladeFile封装类
	 * @param file
	 * @param path
	 * @return
	 */
	public BladeFile getFile(MultipartFile file, String path){
		return new BladeFile(file, path);
	}
	
	/**
	 * 获取BladeFile封装类
	 * @param files
	 * @return
	 */
	public List<BladeFile> getFiles(List<MultipartFile> files){
		return getFiles(files, null);
	}
	
	/**
	 * 获取BladeFile封装类
	 * @param files
	 * @param path
	 * @return
	 */
	public List<BladeFile> getFiles(List<MultipartFile> files, String path){
		List<BladeFile> list = new ArrayList<>();
		for (MultipartFile file : files){
			list.add(new BladeFile(file, path));
		}
		return list;
	}
	
	/**============================     render    =================================================  */
	
	private static final RenderFactory renderFactory = RenderFactory.me();

	/**
	 * 自动注入的response在多线程下会有问题,所以采用手动注入的方式
	 */
	public void render(HttpServletResponse response, Render render) {
		render.setContext(request, response);
		render.render();
	}

	/**
	 * 转向beetl视图
	 */
	public void render(HttpServletResponse response, String view) {
		Render render = renderFactory.getBeetlRender(view);
		render(response, render);
	}

	/**
	 * 输出text
	 */
	public void renderText(HttpServletResponse response, String text) {
		Render render = renderFactory.getTextRender(text);
		render(response, render);
	}

	/**
	 * 根据contentType输出text
	 * <p>
	 * Example: renderText("&lt;user id='5888'&gt;James&lt;/user&gt;",
	 * "application/xml");
	 */
	public void renderText(HttpServletResponse response, String text, String contentType) {
		Render render = renderFactory.getTextRender(text, contentType);
		render(response, render);
	}

	/**
	 * 输出javascript
	 */
	public void renderJavascript(HttpServletResponse response, String javascriptText) {
		Render render = renderFactory.getJavascriptRender(javascriptText);
		render(response, render);
	}

	/**
	 * 输出html
	 */
	public void renderHtml(HttpServletResponse response, String htmlText) {
		Render render = renderFactory.getHtmlRender(htmlText);
		render(response, render);
	}

	/**
	 * 输出文件
	 */
	public void renderFile(HttpServletResponse response, File file) {
		Render render = renderFactory.getFileRender(file);
		render(response, render);
	}

	/**
	 * 输出验证码
	 */
	public void renderCaptcha(HttpServletResponse response) {
		Render render = renderFactory.getCaptchaRender();
		render(response, render);
	}

	/**
	 * 校验验证码
	 */
	public boolean validateCaptcha(String paraName) {
		return CaptchaRender.validate(this, getPara(paraName));
	}

	/**
	 * 重定向
	 */
	public void redirect(HttpServletResponse response, String url) {
		Render render = renderFactory.getRedirectRender(url);
		render(response, render);
	}

	/**
	 * 重定向
	 */
	public void redirect(HttpServletResponse response, String url, boolean withQueryString) {
		Render render = renderFactory.getRedirectRender(url, withQueryString);
		render(response, render);
	}

	/**
	 * 输出json
	 * <p>
	 * Example:<br>
	 * renderJson("message", "Save successful");<br>
	 * renderJson("users", users);<br>
	 */
	public void renderJson(HttpServletResponse response, String key, Object value) {
		Render render = renderFactory.getJsonRender(key, value);
		render(response, render);
	}


	/**
	 * 输出json
	 * <p>
	 * Example: renderJson("{\"message\":\"Please input password!\"}");
	 */
	public void renderJson(HttpServletResponse response, String jsonText) {
		Render render = renderFactory.getJsonRender(jsonText);
		render(response, render);
	}

	/**
	 * 输出json
	 * <p>
	 * Example: renderJson(Maps.create().set("name", "abc").set("age", 18));
	 */
	public void renderJson(HttpServletResponse response, Object object) {
		Render render = object instanceof JsonRender ? (JsonRender) object : renderFactory.getJsonRender(object);
		render(response, render);
	}

	/**
	 * 返回封装的成功信息
	 * 
	 * @param message
	 */
	public void renderSuccess(HttpServletResponse response, String message) {
		renderJson(response, success(message));
	}

	/**
	 * 返回封装的错误信息
	 * 
	 * @param message
	 */
	public void renderError(HttpServletResponse response, String message) {
		renderJson(response, error(message));
	}

	/**
	 * 返回封装的警告信息
	 * 
	 * @param message
	 */
	public void renderWarn(HttpServletResponse response, String message) {
		renderJson(response, warn(message));
	}

	/**
	 * 返回封装的失败信息
	 * 
	 * @param message
	 */
	public void renderFail(HttpServletResponse response, String message) {
		renderJson(response, fail(message));
	}

	/**
	 * 输出excel
	 */
	public void renderExcel(HttpServletResponse response, HSSFWorkbook workbook, String fileName) {
		Render render = renderFactory.getExcelRender(workbook, fileName);
		render(response, render);
	}

	
	/**   
	 * 返回ajaxresult
	 * @param data
	 * @return AjaxResult
	*/
	public AjaxResult json(Object data) {
		return new AjaxResult().success(data);
	}
	
	/**   
	 * 返回ajaxresult
	 * @param data
	 * @param message
	 * @return AjaxResult
	*/
	public AjaxResult json(Object data, String message) {
		return json(data).setMessage(message);
	}
	
	/**   
	 * 返回ajaxresult
	 * @param data
	 * @param message
	 * @param code
	 * @return AjaxResult
	*/
	public AjaxResult json(Object data, String message, int code) {
		return json(data, message).setCode(code);
	}
	
	/**   
	 * 返回ajaxresult
	 * @param message
	 * @return AjaxResult
	*/
	public AjaxResult success(String message) {
		return new AjaxResult().addSuccess(message);
	}
	
	/**   
	 * 返回ajaxresult
	 * @param message
	 * @return AjaxResult
	*/
	public AjaxResult error(String message) {
		return new AjaxResult().addError(message);
	}
	
	/**   
	 * 返回ajaxresult
	 * @param message
	 * @return AjaxResult
	*/
	public AjaxResult warn(String message) {
		return new AjaxResult().addWarn(message);
	}
	
	/**   
	 * 返回ajaxresult
	 * @param message
	 * @return AjaxResult
	*/
	public AjaxResult fail(String message) {
		return new AjaxResult().addFail(message);
	}
	
	/** ============================     cookie    =================================================  */
	
	/**
	 * Get cookie value by cookie name.
	 */
	public String getCookie(String name, String defaultValue) {
		Cookie cookie = getCookieObject(name);
		return cookie != null ? cookie.getValue() : defaultValue;
	}

	/**
	 * Get cookie value by cookie name.
	 */
	public String getCookie(String name) {
		return getCookie(name, null);
	}

	/**
	 * Get cookie value by cookie name and convert to Integer.
	 */
	public Integer getCookieToInt(String name) {
		String result = getCookie(name);
		return result != null ? Integer.parseInt(result) : null;
	}

	/**
	 * Get cookie value by cookie name and convert to Integer.
	 */
	public Integer getCookieToInt(String name, Integer defaultValue) {
		String result = getCookie(name);
		return result != null ? Integer.parseInt(result) : defaultValue;
	}

	/**
	 * Get cookie value by cookie name and convert to Long.
	 */
	public Long getCookieToLong(String name) {
		String result = getCookie(name);
		return result != null ? Long.parseLong(result) : null;
	}

	/**
	 * Get cookie value by cookie name and convert to Long.
	 */
	public Long getCookieToLong(String name, Long defaultValue) {
		String result = getCookie(name);
		return result != null ? Long.parseLong(result) : defaultValue;
	}

	/**
	 * Get cookie object by cookie name.
	 */
	public Cookie getCookieObject(String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null)
			for (Cookie cookie : cookies)
				if (cookie.getName().equals(name))
					return cookie;
		return null;
	}

	/**
	 * Get all cookie objects.
	 */
	public Cookie[] getCookieObjects() {
		Cookie[] result = request.getCookies();
		return result != null ? result : new Cookie[0];
	}

	/**
	 * Set Cookie.
	 * 
	 * @param name
	 *            cookie name
	 * @param value
	 *            cookie value
	 * @param maxAgeInSeconds
	 *            -1: clear cookie when close browser. 0: clear cookie
	 *            immediately. n>0 : max age in n seconds.
	 * @param isHttpOnly
	 *            true if this cookie is to be marked as HttpOnly, false
	 *            otherwise
	 */
	public BladeController setCookie(String name, String value, int maxAgeInSeconds, boolean isHttpOnly) {
		return doSetCookie(name, value, maxAgeInSeconds, null, null, isHttpOnly);
	}

	/**
	 * Set Cookie.
	 * 
	 * @param name
	 *            cookie name
	 * @param value
	 *            cookie value
	 * @param maxAgeInSeconds
	 *            -1: clear cookie when close browser. 0: clear cookie
	 *            immediately. n>0 : max age in n seconds.
	 */
	public BladeController setCookie(String name, String value, int maxAgeInSeconds) {
		return doSetCookie(name, value, maxAgeInSeconds, null, null, null);
	}

	/**
	 * Set Cookie to response.
	 */
	public BladeController setCookie(Cookie cookie) {
		response.addCookie(cookie);
		return this;
	}

	/**
	 * Set Cookie to response.
	 * 
	 * @param name
	 *            cookie name
	 * @param value
	 *            cookie value
	 * @param maxAgeInSeconds
	 *            -1: clear cookie when close browser. 0: clear cookie
	 *            immediately. n>0 : max age in n seconds.
	 * @param path
	 *            see Cookie.setPath(String)
	 * @param isHttpOnly
	 *            true if this cookie is to be marked as HttpOnly, false
	 *            otherwise
	 */
	public BladeController setCookie(String name, String value, int maxAgeInSeconds, String path, boolean isHttpOnly) {
		return doSetCookie(name, value, maxAgeInSeconds, path, null, isHttpOnly);
	}

	/**
	 * Set Cookie to response.
	 * 
	 * @param name
	 *            cookie name
	 * @param value
	 *            cookie value
	 * @param maxAgeInSeconds
	 *            -1: clear cookie when close browser. 0: clear cookie
	 *            immediately. n>0 : max age in n seconds.
	 * @param path
	 *            see Cookie.setPath(String)
	 */
	public BladeController setCookie(String name, String value, int maxAgeInSeconds, String path) {
		return doSetCookie(name, value, maxAgeInSeconds, path, null, null);
	}

	/**
	 * Set Cookie to response.
	 * 
	 * @param name
	 *            cookie name
	 * @param value
	 *            cookie value
	 * @param maxAgeInSeconds
	 *            -1: clear cookie when close browser. 0: clear cookie
	 *            immediately. n>0 : max age in n seconds.
	 * @param path
	 *            see Cookie.setPath(String)
	 * @param domain
	 *            the domain name within which this cookie is visible; form is
	 *            according to RFC 2109
	 * @param isHttpOnly
	 *            true if this cookie is to be marked as HttpOnly, false
	 *            otherwise
	 */
	public BladeController setCookie(String name, String value, int maxAgeInSeconds, String path, String domain, boolean isHttpOnly) {
		return doSetCookie(name, value, maxAgeInSeconds, path, domain, isHttpOnly);
	}

	/**
	 * Remove Cookie.
	 */
	public BladeController removeCookie(String name) {
		return doSetCookie(name, null, 0, null, null, null);
	}

	/**
	 * Remove Cookie.
	 */
	public BladeController removeCookie(String name, String path) {
		return doSetCookie(name, null, 0, path, null, null);
	}

	/**
	 * Remove Cookie.
	 */
	public BladeController removeCookie(String name, String path, String domain) {
		return doSetCookie(name, null, 0, path, domain, null);
	}

	private BladeController doSetCookie(String name, String value, int maxAgeInSeconds, String path, String domain, Boolean isHttpOnly) {
		Cookie cookie = new Cookie(name, value);
		cookie.setMaxAge(maxAgeInSeconds);
		// set the default path value to "/"
		if (path == null) {
			path = "/";
		}
		cookie.setPath(path);

		if (domain != null) {
			cookie.setDomain(domain);
		}
		if (isHttpOnly != null) {
			cookie.setHttpOnly(isHttpOnly);
		}
		response.addCookie(cookie);
		return this;
	}
	
	/** ============================     diy    =================================================  */
	
	private Object basepage(String slaveName, String source, IQuery intercept){
		Integer page = getParaToInt("page", 1);
		Integer rows = getParaToInt("rows", 10);
		String where = getPara("where", "");
		String sidx =  getPara("sidx", "");
		String sord =  getPara("sord", "");
		String sort =  getPara("sort", "");
		String order =  getPara("order", "");
		if (StrKit.notBlank(sidx)) {
			sort = sidx + " " + sord
					+ (StrKit.notBlank(sort) ? ("," + sort) : "");
		}
		Object grid = GridManager.paginate(slaveName, page, rows, source, where, sort, order, intercept, this);
		return grid;
	}
	
	/**
	 * @param 数据源
	 * @return
	 */
	protected Object paginate(String source){
		return basepage(null, source, Cst.me().getDefaultPageFactory());
	}
	
	/**
	 * @param 数据源
	 * @param 自定义拦截器
	 * @return
	 */
	protected Object paginate(String source, IQuery intercept){
		return basepage(null, source, intercept);
	}
	
	/**
	 * @param 数据库别名
	 * @param 数据源
	 * @return
	 */
	protected Object paginate(String slaveName, String source){
		return basepage(slaveName, source, Cst.me().getDefaultPageFactory());
	}
	
	/**
	 * @param 数据库别名
	 * @param 数据源
	 * @param 自定义拦截器
	 * @return
	 */
	protected Object paginate(String slaveName, String source, IQuery intercept){
		return basepage(slaveName, source, intercept);
	}

}
