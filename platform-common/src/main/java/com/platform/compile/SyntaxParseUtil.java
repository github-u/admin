package com.platform.compile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ql.util.express.ExpressUtil;
import com.ql.util.express.parse.ExpressNode;
import com.ql.util.express.parse.ExpressPackage;
import com.ql.util.express.parse.NodeType;
import com.ql.util.express.parse.NodeTypeManager;
import com.ql.util.express.parse.Word;

public class SyntaxParseUtil {

    NodeTypeManager nodeTypeManager;

    boolean isPrecise;

    public List<ExpressNode> transferWord2ExpressNode(ExpressPackage aRootExpressPackage,Word[] wordObjects,Map<String,String> selfClassDefine,boolean dealJavaClass) throws Exception{
        List<ExpressNode> result = new ArrayList<ExpressNode>();
        String tempWord;
        NodeType tempType;
        int point = 0;
        ExpressPackage  tmpImportPackage = null;
        if(dealJavaClass==true){
            tmpImportPackage = new ExpressPackage(aRootExpressPackage);  
            //先处理import，import必须放在文件的最开始，必须以;结束
            boolean isImport = false;
            StringBuffer importName = new StringBuffer();
            while(point <wordObjects.length ){
                if(wordObjects[point].word.equals("import") ==true){
                    isImport = true;
                    importName.setLength(0);
                }else if(wordObjects[point].word.equals(";") ==true) {
                    isImport = false;
                    tmpImportPackage.addPackage(importName.toString());
                }else if(isImport == true){
                    importName.append(wordObjects[point].word);
                }else{
                    break;
                }
                point = point + 1;
            }			
        }

        String orgiValue = null;
        Object objectValue = null;
        NodeType treeNodeType = null;
        Word tmpWordObject = null;
        while(point <wordObjects.length){
            tmpWordObject = wordObjects[point];
            tempWord = wordObjects[point].word;

            char firstChar = tempWord.charAt(0);
            char lastChar = tempWord.substring(tempWord.length() - 1).toLowerCase().charAt(0);		  
            if(firstChar >='0' && firstChar<='9'){
                if(result.size() >0){//对 负号进行特殊处理
                    if(result.get(result.size() -1).getValue().equals("-")){
                        if(result.size() == 1 
                                || result.size() >=2 
                                && (   result.get(result.size() - 2).isTypeEqualsOrChild("OP_LIST")
                                        || result.get(result.size() - 2).isTypeEqualsOrChild(",")
                                        || result.get(result.size() - 2).isTypeEqualsOrChild("return")
                                        || result.get(result.size() - 2).isTypeEqualsOrChild("?")
                                        || result.get(result.size() - 2).isTypeEqualsOrChild(":")
                                        ) 
                                && result.get(result.size() - 2).isTypeEqualsOrChild(")")==false
                                && result.get(result.size() - 2).isTypeEqualsOrChild("]")==false 
                                ){
                            result.remove(result.size() -1);
                            tempWord = "-" + tempWord;
                        }
                    }
                }
                if(lastChar =='d'){
                    tempType = nodeTypeManager.findNodeType("CONST_DOUBLE");
                    tempWord = tempWord.substring(0,tempWord.length() -1);
                    if(this.isPrecise == true){
                        objectValue = new BigDecimal(tempWord);
                    }else{
                        objectValue = Double.valueOf(tempWord);
                    }
                }else if(lastChar =='f'){
                    tempType = nodeTypeManager.findNodeType("CONST_FLOAT");
                    tempWord = tempWord.substring(0,tempWord.length() -1);
                    if(this.isPrecise == true){
                        objectValue = new BigDecimal(tempWord);
                    }else{
                        objectValue = Float.valueOf(tempWord);
                    }
                }else if(tempWord.indexOf(".") >=0){
                    tempType = nodeTypeManager.findNodeType("CONST_DOUBLE");
                    if(this.isPrecise == true){
                        objectValue = new BigDecimal(tempWord);
                    }else{
                        objectValue = Double.valueOf(tempWord);
                    }
                }else if(lastChar =='l'){
                    tempType = nodeTypeManager.findNodeType("CONST_LONG");
                    tempWord = tempWord.substring(0,tempWord.length() -1);
                    objectValue = Long.valueOf(tempWord);
                }else{
                    long tempLong = Long.parseLong(tempWord);
                    if(tempLong <= Integer.MAX_VALUE && tempLong >= Integer.MIN_VALUE){
                        tempType = nodeTypeManager.findNodeType("CONST_INTEGER");
                        objectValue = Integer.valueOf((int)tempLong);
                    }else{
                        tempType = nodeTypeManager.findNodeType("CONST_LONG");
                        objectValue = Long.valueOf(tempLong);
                    }
                }
                treeNodeType = nodeTypeManager.findNodeType("CONST");
                point = point + 1;
            }else if(firstChar =='"'){
                if(lastChar !='"' || tempWord.length() <2){
                    throw new Exception("没有关闭的字符串：" + tempWord);
                }
                tempWord = tempWord.substring(1,tempWord.length() -1);
                tempType =nodeTypeManager.findNodeType("CONST_STRING");
                objectValue = tempWord;
                treeNodeType = nodeTypeManager.findNodeType("CONST");
                point = point + 1;
            }else if(firstChar =='\''){
                if(lastChar !='\'' || tempWord.length() <2){
                    throw new Exception("没有关闭的字符：" + tempWord);
                }
                tempWord = tempWord.substring(1,tempWord.length() -1);

                treeNodeType = nodeTypeManager.findNodeType("CONST");
                if(tempWord.length() == 1){ //转换为字符串
                    tempType =nodeTypeManager.findNodeType("CONST_CHAR");
                    objectValue = tempWord.charAt(0);
                }else{
                    tempType =nodeTypeManager.findNodeType("CONST_STRING");
                    objectValue = tempWord;
                }

                point = point + 1;
            }else if(tempWord.equals("true") || tempWord.equals("false")){
                tempType = nodeTypeManager.findNodeType("CONST_BOOLEAN");
                treeNodeType = nodeTypeManager.findNodeType("CONST");
                objectValue = Boolean.valueOf(tempWord);
                point = point + 1;
            }else {
                tempType = nodeTypeManager.isExistNodeTypeDefine(tempWord);
                if(tempType != null && tempType.getKind() != null /**NodeTypeKind.KEYWORD*/){
                    //不是关键字
                    tempType = null;
                }
                if (tempType == null) {
                    boolean isClass = false;
                    String tmpStr = "";
                    Class<?> tmpClass = null;
                    if (dealJavaClass == true) {
                        int j = point;
                        while (j < wordObjects.length) {
                            tmpStr = tmpStr + wordObjects[j].word;
                            tmpClass = tmpImportPackage.getClass(tmpStr);
                            if (tmpClass != null) {
                                point = j + 1;
                                isClass = true;
                                break;
                            }
                            if (j < wordObjects.length - 1
                                    && wordObjects[j + 1].word.equals(".") == true) {
                                tmpStr = tmpStr + wordObjects[j + 1].word;
                                j = j + 2;
                                continue;
                            } else {
                                break;
                            }
                        }
                    }
                    if (isClass == true){
                        tempWord = ExpressUtil.getClassName(tmpClass);
                        orgiValue = tmpStr;
                        tempType = nodeTypeManager.findNodeType("CONST_CLASS");
                        objectValue = tmpClass;
                    }else if(this.nodeTypeManager.isFunction(tempWord)){
                        tempType = nodeTypeManager.findNodeType("FUNCTION_NAME");
                        point = point + 1;
                    }else if(selfClassDefine != null && selfClassDefine.containsKey(tempWord)){
                        tempType = nodeTypeManager.findNodeType("VClass");
                        point = point + 1;
                    }else{
                        tempType = nodeTypeManager.findNodeType("ID");
                        point = point + 1;
                    }
                }else{
                    point = point + 1;
                }
            }	  
            result.add(new ExpressNode(tempType,tempWord,orgiValue,objectValue,treeNodeType,tmpWordObject.line,tmpWordObject.col,tmpWordObject.index));
            treeNodeType = null;
            objectValue = null;
            orgiValue = null;
        }
        return result;
    }
}
