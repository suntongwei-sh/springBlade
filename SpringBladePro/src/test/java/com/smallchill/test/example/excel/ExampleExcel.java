package com.smallchill.test.example.excel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.jeecgframework.poi.excel.ExcelExportUtil;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.params.ExcelExportEntity;
import org.jeecgframework.poi.excel.entity.vo.MapExcelConstants;
import org.jeecgframework.poi.excel.entity.vo.NormalExcelConstants;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smallchill.core.base.controller.BladeController;

public class ExampleExcel extends BladeController {

	/**
	 * renderExcel封装方式
	 */
	@RequestMapping("/export")
	public void exportMerchantProfitQuery(HttpServletResponse response) {
		List<ExcelExportEntity> entityList = new ArrayList<ExcelExportEntity>();
		entityList.add(new ExcelExportEntity("姓名", "name"));
		entityList.add(new ExcelExportEntity("性别", "sex"));
		entityList.add(new ExcelExportEntity("国籍", "country"));

		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		Map<String, String> map;
		for (int i = 0; i < 10; i++) {
			map = new HashMap<String, String>();
			map.put("name", "1" + i);
			map.put("sex", "2" + i);
			map.put("country", "大中华");
			list.add(map);
		}
		HSSFWorkbook workbook = (HSSFWorkbook) ExcelExportUtil.exportExcel(new ExportParams("测试", "sheet1"), entityList, list);
		renderExcel(response, workbook, "2333");
	}

	/**
	 * excel视图方式
	 */
	@RequestMapping("/export1")
	public String exportMerchantProfitQuery(ModelMap modelMap,
			HttpServletResponse response) {
		List<ExcelExportEntity> entityList = new ArrayList<ExcelExportEntity>();
		entityList.add(new ExcelExportEntity("商户名称", "merchantName", 35));
		entityList.add(new ExcelExportEntity("商户号", "merchantId", 15));
		entityList.add(new ExcelExportEntity("商户类型", "merchantType", 15));
		entityList.add(new ExcelExportEntity("联系人", "userName", 15));
		entityList.add(new ExcelExportEntity("总分润", "allProfit", 15));
		List<Map<String, String>> dataResult = new ArrayList<Map<String, String>>();
		Map<String, String> map;
		for (int i = 0; i < 10; i++) {
			map = new HashMap<String, String>();
			map.put("merchantName", "1" + i);
			map.put("merchantId", "2" + i);
			map.put("merchantType", "大中华");
			map.put("userName", "abc" + i);
			map.put("allProfit", "def" + i);
			dataResult.add(map);
		}

		modelMap.put(MapExcelConstants.ENTITY_LIST, entityList);
		modelMap.put(MapExcelConstants.MAP_LIST, dataResult);
		modelMap.put(MapExcelConstants.FILE_NAME, "商户利润");
		modelMap.put(NormalExcelConstants.PARAMS, new ExportParams("商户利润详情", "商户"));
		return MapExcelConstants.JEECG_MAP_EXCEL_VIEW;
	}

}
