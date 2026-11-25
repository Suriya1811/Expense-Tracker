package com.suriya.expensetracker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/** Minimal XLSX reader/writer for visible monthly sheets and one restoration sheet. */
final class WorkbookIO {
    static final String META="App data";
    static final int MAX_ENTRY=20*1024*1024, MAX_ROWS=200000;
    static final class Cell {final String value;final boolean number;Cell(String value,boolean number){this.value=value==null?"":value;this.number=number;}}
    static Cell t(String value){return new Cell(value,false);}static Cell n(String value){return new Cell(value,true);}
    static final class Sheet {final String name;final ArrayList<List<Cell>> rows=new ArrayList<>();Sheet(String name){this.name=name;}void row(Cell... cells){rows.add(Arrays.asList(cells));}}
    static String esc(String s){StringBuilder b=new StringBuilder();for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c=='&')b.append("&amp;");else if(c=='<')b.append("&lt;");else if(c=='>')b.append("&gt;");else if(c=='\"')b.append("&quot;");else if(c=='\'' )b.append("&apos;");else if(c=='\n'||c=='\r'||c=='\t'||c>=0x20)b.append(c);}return b.toString();}
    static String col(int n){StringBuilder b=new StringBuilder();for(n++;n>0;n=(n-1)/26)b.insert(0,(char)('A'+(n-1)%26));return b.toString();}
    static void entry(ZipOutputStream z,String name,String text)throws IOException{z.putNextEntry(new ZipEntry(name));z.write(text.getBytes(StandardCharsets.UTF_8));z.closeEntry();}
    static void write(OutputStream out,List<Sheet> sheets)throws IOException{
        if(sheets.isEmpty())throw new IOException("No worksheets");
        try(ZipOutputStream z=new ZipOutputStream(new BufferedOutputStream(out))){
            StringBuilder types=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
            StringBuilder wb=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
            StringBuilder rel=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
            for(int i=0;i<sheets.size();i++){int id=i+1;types.append("<Override PartName=\"/xl/worksheets/sheet").append(id).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");wb.append("<sheet name=\"").append(esc(sheets.get(i).name)).append("\" sheetId=\"").append(id).append("\" r:id=\"rId").append(id).append("\"/>");rel.append("<Relationship Id=\"rId").append(id).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(id).append(".xml\"/>");}
            types.append("</Types>");wb.append("</sheets></workbook>");rel.append("<Relationship Id=\"rId").append(sheets.size()+1).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
            entry(z,"[Content_Types].xml",types.toString());entry(z,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");entry(z,"xl/workbook.xml",wb.toString());entry(z,"xl/_rels/workbook.xml.rels",rel.toString());
            entry(z,"xl/styles.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><color rgb=\"FFFFFFFF\"/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts><fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF263B52\"/><bgColor indexed=\"64\"/></patternFill></fill></fills><borders count=\"1\"><border/></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/></cellXfs></styleSheet>");
            for(int i=0;i<sheets.size();i++){Sheet sheet=sheets.get(i);z.putNextEntry(new ZipEntry("xl/worksheets/sheet"+(i+1)+".xml"));Writer w=new OutputStreamWriter(z,StandardCharsets.UTF_8);w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><cols><col min=\"1\" max=\"1\" width=\"24\" customWidth=\"1\"/><col min=\"2\" max=\"12\" width=\"24\" customWidth=\"1\"/></cols><sheetData>");int rn=0;for(List<Cell> row:sheet.rows){rn++;w.write("<row r=\""+rn+"\">");for(int c=0;c<row.size();c++){Cell cell=row.get(c);if(cell==null)continue;String ref=col(c)+rn,style=rn==1?" s=\"1\"":"";if(cell.number&&cell.value.matches("-?[0-9]+(?:\\.[0-9]+)?"))w.write("<c r=\""+ref+"\""+style+"><v>"+cell.value+"</v></c>");else w.write("<c r=\""+ref+"\" t=\"inlineStr\""+style+"><is><t xml:space=\"preserve\">"+esc(cell.value)+"</t></is></c>");}w.write("</row>");}w.write("</sheetData></worksheet>");w.flush();z.closeEntry();}
        }
    }
    static byte[] bytes(ZipFile z,ZipEntry e)throws IOException{if(e==null)throw new IOException("Missing Excel data");if(e.getSize()>MAX_ENTRY)throw new IOException("Excel entry too large");try(InputStream in=z.getInputStream(e);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>MAX_ENTRY)throw new IOException("Excel entry too large");out.write(b,0,n);}return out.toByteArray();}}
    static Document xml(byte[] b)throws Exception{
        // Android's XML parser does not support every Apache feature. XLSX XML is UTF-8;
        // reject DTDs before parsing and refuse external entities on every parser.
        for(byte value:b)if(value==0)throw new IOException("Unsupported XML encoding");
        String source=new String(b,StandardCharsets.UTF_8);
        if(source.toUpperCase(Locale.ROOT).contains("<!DOCTYPE")||source.toUpperCase(Locale.ROOT).contains("<!ENTITY"))throw new IOException("XML declarations are not allowed");
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(false);
        for(String feature:new String[]{"http://apache.org/xml/features/disallow-doctype-decl","http://xml.org/sax/features/external-general-entities","http://xml.org/sax/features/external-parameter-entities"}){
            try{f.setFeature(feature,feature.endsWith("disallow-doctype-decl"));}catch(ParserConfigurationException | IllegalArgumentException ignored){}
        }
        DocumentBuilder builder=f.newDocumentBuilder();builder.setEntityResolver((publicId,systemId)->{throw new org.xml.sax.SAXException("External entities are not allowed");});
        return builder.parse(new InputSource(new StringReader(source)));
    }
    static ArrayList<String[]> readMeta(File file)throws Exception{
        try(ZipFile z=new ZipFile(file)){Document workbook=xml(bytes(z,z.getEntry("xl/workbook.xml")));NodeList sheets=workbook.getElementsByTagName("sheet");String relId=null;for(int i=0;i<sheets.getLength();i++){Element e=(Element)sheets.item(i);if(META.equals(e.getAttribute("name"))){if(relId!=null)throw new IOException("Multiple app data sheets");relId=e.getAttribute("r:id");}}if(relId==null)throw new IOException("App data sheet not found");Document rels=xml(bytes(z,z.getEntry("xl/_rels/workbook.xml.rels")));NodeList links=rels.getElementsByTagName("Relationship");String target=null;for(int i=0;i<links.getLength();i++){Element e=(Element)links.item(i);if(relId.equals(e.getAttribute("Id"))&&!"External".equals(e.getAttribute("TargetMode")))target=e.getAttribute("Target");}if(target==null||target.contains("..")||target.contains("\\"))throw new IOException("Invalid worksheet path");String path=target.startsWith("/")?target.substring(1):target.startsWith("xl/")?target:"xl/"+target;ArrayList<String> shared=new ArrayList<>();ZipEntry sharedEntry=z.getEntry("xl/sharedStrings.xml");if(sharedEntry!=null){Document ss=xml(bytes(z,sharedEntry));NodeList list=ss.getElementsByTagName("si");for(int i=0;i<list.getLength();i++)shared.add(list.item(i).getTextContent());}
            Document sheet=xml(bytes(z,z.getEntry(path)));NodeList rows=sheet.getElementsByTagName("row");if(rows.getLength()>MAX_ROWS)throw new IOException("Too many backup rows");ArrayList<String[]> result=new ArrayList<>();for(int i=0;i<rows.getLength();i++){NodeList cells=((Element)rows.item(i)).getElementsByTagName("c");String[] row={"",""};for(int j=0;j<cells.getLength();j++){Element c=(Element)cells.item(j);String ref=c.getAttribute("r");int column=ref.startsWith("A")?0:ref.startsWith("B")?1:-1;if(column<0||ref.length()>1&&Character.isLetter(ref.charAt(1)))continue;String type=c.getAttribute("t"),value="";NodeList vals=c.getElementsByTagName(type.equals("inlineStr")?"t":"v");if(vals.getLength()>0)value=vals.item(0).getTextContent();if(type.equals("s")){int index=Integer.parseInt(value);if(index<0||index>=shared.size())throw new IOException("Invalid shared string");value=shared.get(index);}row[column]=value;}result.add(row);}return result;
        }
    }
}
