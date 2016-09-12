/**
 * Copyright (c) 2015-2016, Chill Zhuang 庄骞 (smallchill@163.com).
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
package com.smallchill.core.controller;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.smallchill.core.base.controller.BladeController;
import com.smallchill.core.constant.Cst;
import com.smallchill.core.plugins.dao.Db;
import com.smallchill.core.toolbox.Func;
import com.smallchill.core.toolbox.Record;
import com.smallchill.core.toolbox.ajax.AjaxResult;
import com.smallchill.core.toolbox.file.BladeFile;
import com.smallchill.core.toolbox.file.UploadFileUtils;
import com.smallchill.core.toolbox.kit.PathKit;

@Controller
@RequestMapping("/kindeditor")
public class KindEditorController extends BladeController {
	
	@ResponseBody
	@RequestMapping("/upload_json")
	public Record upload_json(@RequestParam("imgFile") MultipartFile file) {
		Record maps = Record.create();
		if (null == file) {
			maps.set("error", 1);
			maps.set("message", "请选择要上传的图片");
			return maps;
		}
		String originalFileName = file.getOriginalFilename();
		String dir = getPara("dir", "image");
		// 测试后缀
		boolean ok = UploadFileUtils.testExt(dir, originalFileName);
		if (!ok) {
			maps.set("error", 1);
			maps.set("message", "上传文件的类型不允许");
			return maps;
		}
		BladeFile bf = getFile(file);
		bf.transfer();
		Object fileId = bf.getFileId();	
		String url = "/kindeditor/renderFile/" + fileId;
		maps.set("error", 0);
		maps.set("title", fileId);
		maps.set("url", Cst.me().getContextPath() + url);
		maps.set("name", originalFileName);
		return maps;	
	}
	
	@ResponseBody
	@RequestMapping("/file_manager_json")
	public Object file_manager_json() {
		String dir = getPara("dir", "image");
		// 考虑安全问题
		String path = getPara("path", "");
		// 不允许使用..移动到上一级目录
		if (path.indexOf("..") >= 0) {
			return "Access is not allowed.";
		}
		// 最后一个字符不是/
		if (!"".equals(path) && !path.endsWith("/")) {
			return "Parameter is not valid.";
		}
		String order = getPara("order", "name");

		Map<String, Object> result = UploadFileUtils.listFiles(dir, path, order);
		return result;
	}
	
	@ResponseBody
	@RequestMapping("/initimg")
	public AjaxResult initimg(@RequestParam String id) { 
		Map<String, Object> img = Db.init().findById("TFW_ATTACH", id);
		if (null != img) {
			return json(img);
		} else {
			return fail("获取图片失败！");
		}
	}

	@ResponseBody
	@RequestMapping("/initfile")
	public AjaxResult initfile(@RequestParam String ids) {
		Db dao = Db.init();
		List<Record> file = dao.selectList("select ID as \"id\",NAME as \"name\",URL as \"url\" from TFW_ATTACH where ID in (#{join(ids)})", Record.create().set("ids", ids.split(",")));
		if (null != file) {
			return json(file);
		} else {
			return fail("获取附件失败！");
		}
	}

	@RequestMapping("/renderFile/{id}")
	public void renderFile(HttpServletResponse response, @PathVariable String id) {
		Map<String, Object> file = Db.init().findById("TFW_ATTACH", id);
		if (Func.isEmpty(file)) {
			redirect(response, "/error/error404");
			return;
		} else {
			String url = file.get("URL").toString();
			File f = new File((Cst.me().isRemoteMode() ? "" : PathKit.getWebRootPath()) + url);
			renderFile(response, f);
		}
	}
	
	
}
