package com.awt.anlyxml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.awt.domain.BasiDoMain;
import com.awt.domain.DoMain;
import com.awt.domain.Navig;
import com.awt.util.Print;
import com.awt.util.Util;
import com.awt.util.UtilBeanContext;
import com.bean.support.SetterGetter;
/**
 * <b>解析xml文件生成domain对象</b>
 * <p>
 * 描述:
 * <p>
 * 通过beans获取实例化的domain对象，因此要在beans配置文件中配置相关bean数据信息 ，beanID要和相应的导航对象对应（即标签名称），达到解耦。
 * <p>
 * 生成domain数据通过beans的setter、getter方法对相应的属性反射写入属性值
 * @author 威 
 * <br>2018年4月13日 下午11:01:54 
 * @see com.bean.support.SetterGetter
 * @see com.awt.util.UtilBeanContext
 * @see com.awt.anlyxml.AbstractAnalyXML
 * @since 1.0
 */
public class TestXML extends AbstractAnalyXML {
	//启动解析
	public DoMain doStart(){
		Navig navig = this.navig;
		return dealStep1(this.doc, navig);
	}
	
	/**
	 * 获取NodeList对象
	 * <p>	 
	 * @param parentEle	父节点
	 * @param naivg	导航对象
	 * @return
	 * Object 当前的domain对象
	 * @see #doStart()
	 * @see #dealStep2(NodeList, Navig)
	 * @since 1.0
	 */
	protected DoMain dealStep1(Object parentEle, Navig naivg) {
		return dealStep1(null, parentEle, naivg);
	}
	protected DoMain dealStep1(DoMain parentDoMain,Object parentEle, Navig naivg) {
		NodeList childNodes = parentEle instanceof Document
				? ((Document) parentEle).getElementsByTagName(naivg.name)
				: ((Element) parentEle).getChildNodes();
		return dealStep2(parentDoMain,childNodes, naivg);
	}

	private int num = 0;
	/**
	 * 处理NodeList对象中的子元素
	 * <p>
	 * 通过beans获取实例化的domain对象，因此要在beans配置文件中配置相关bean数据信息 ，beanID要和相应的导航对象对应（即标签名称），达到解耦。
	 * @param nodes	子节点NodeList对象
	 * @param naivg	导航对象
	 * @return
	 * Object 当前的domain对象
	 * @see	#dealStep1(Object, Navig)
	 * @see #dealAttrs(NamedNodeMap, Object)
	 * @since 1.0
	 */
	protected DoMain dealStep2(DoMain parentDoMain, NodeList nodes, Navig naivg) {
		DoMain object = null;
		int num = this.num;
		try {
			int len = nodes.getLength();
			if (len > 0) {
				for (int i = 0; i < len; i++) {
					Node node = nodes.item(i);
					Print.out(this,
							"dealStep2-获取子节点",
							getSep(num) +
							"导航对象名称：" + naivg.name +
							"；nodes长度：" + len +
							"；childNodeName：" + node.getNodeName() +
							"；navigSize：" + naivg.next.size() +
							"；导航对象字符串：" + naivg);
					if (node.getNodeType() == 1 && node.getNodeName().equals(naivg.name)) {
						object = (DoMain) UtilBeanContext.getBean(naivg.name);
						Element element = (Element) node;
						dealAttrs(element.getAttributes(), object); // 解析attrs
						Print.out(this,
								"dealStep2-已判断是符合的节点", 
								"第几个元素：" + i +
								"；domain对象名称：" + object.getClass().getSimpleName() +
								"；domain对象：" + object);
						if (parentDoMain != null) {
							Print.out(this,
									"dealStep2-注入递归获取到domain",
									new StringBuilder(naivg.name)
									.append("(")
									.append("组件名称：")
									.append(((BasiDoMain) object).getName())
									.append(")")
									.append("-")
									.append("将要注入对象：")
									.append(naivg.name)
									.append("(")
									.append(((BasiDoMain) object).getName())
									.append(")"));
							SetterGetter.setProperty(parentDoMain,
									naivg.name,
									object);
						}
						if (naivg.next != null && naivg.next.size() > 0) {
							num = ++this.num;
							for (Navig naivg1 : naivg.next) {
								dealStep1(object, element, naivg1);
							}		
						}
					}
				}
			}
		} catch (Exception e) { 
			e.printStackTrace();
		}
		return object;
	}
	
	public String getSep(int num){
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < num; i++)
			sb.append("-");
		return sb.toString();
	}

	/**
	 * 处理标签中的属性并载入domain对象中
	 * <p>	
	 * 生成domain数据通过beans的setter、getter方法对相应的属性反射写入属性值 
	 * @param attrMaps	标签属性NamedNodeMap对象
	 * @param object 	domain对象，用于放射写入属性值
	 * void
	 * @see	#dealStep2(NodeList, Navig)
	 * @since 1.0
	 */
	protected void dealAttrs(NamedNodeMap attrMaps, Object object) {
		int attrLen = attrMaps.getLength();
		for (int i = 0; i < attrLen; i++) {
			String attr = attrMaps.item(i).toString(); // 得到的数据是 key=value
			int sepIndex = attr.indexOf("=");
			String field = attr.substring(0, sepIndex);
			Object value = getConformType(attr.substring(sepIndex + 1, attr.length()));
			try {
				SetterGetter.setProperty(object, field, value);
			} catch (Exception e) { 
				Print.erro(this, "dealAttrs", "field:"+field);
				e.printStackTrace();}
			//field(field, value);
		}
	}
	
	//将一串字符串转换成符合的类型  例如Integer ， boolean ， 去掉 上引号
	//有异常 转换成Integer时 有上引号
	/**
	 * 输入一串字符串 自动返回对应类型的对象
	 * <p>	 
	 * @param value	需要获取对应类型的对象的字符串
	 * @return
	 * Object		返回对应类型的对象
	 * @see #dealAttrs(NamedNodeMap, Object)
	 * @since 1.0
	 */
	protected Object getConformType(String value) {
		value = value.replaceAll("\"", "");
		if (value.equals("false") || value.equals("true")) {
			return Boolean.parseBoolean(value);
		}
		else if (Util.isNumber(value)) {
			return Integer.parseInt(value);
		}
		else {
			return value;
		}
		
	}
}

