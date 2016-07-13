package com.nff.sqlciphertest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import net.sqlcipher.Cursor;
import net.sqlcipher.SQLException;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabase.CursorFactory;
import net.sqlcipher.database.SQLiteOpenHelper;
import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

/** 
 * <p><b>Description:</b> 本类继承自{@link net.sqlcipher.database.SQLiteOpenHelper},
 * 用于对创建的数据库进行加密解密处理，防止数据库中的数据在ROOT过的设备上进行泄露。
 * 其它项目使用时，请将libs下的包和asserts目录下的zip复制到自己项目中。
 * 本类初始化先调用SQLiteDatabase.loadLibs(this);加载JNI本地库，
 * 然后调用构造器DBHelperUtil.{@link #DBHelperUtil(Context, String, CursorFactory, int)}
 * 便可以获取到数据库的对象，本类中提供了常用的数据库操作方法。
 *  </p>
 * <p><b>ClassName:</b> DBHelperUtil</p> 
 * @author NingFeifei
 * <p><b>date</b> 2016-7-13 下午3:28:57 </p> 
 */
public class DBHelperUtil extends SQLiteOpenHelper {
	
	private final String timeTable ="CREATE TABLE TableRecord(_id integer primary key autoincrement,name varchar(20),datetime integer);";
	//test 正式用时删掉
	public static final String CREATE_TABLE = "create table Book(name text, pages integer)";
	boolean isSucess;
	public static final int INSERT_TYPE_CLEAR=1,	//清空表
			INSERT_TYPE_DELETE_SAME_GROUPID=2,		//删除同group_id的
			INSERT_TYPE_NORMAL=0;					//正常
	private SQLiteDatabase db;
	private Context mContext;
	/** 经过转换的数据库密码(MobieNurseStation)*/ 
	public static String password;
	
	static {
		password = "\u004d\u006f\u0062\u0069\u0065\u004e\u0075\u0072\u0073\u0065\u0053\u0074\u0061\u0074\u0069\u006f\u006e";
	}
	
	/**
	 * 构造方法
	 * @param context 上下文
	 * @param name  数据库名称
	 * @param factory  Cursor工厂 无特殊要求 null即可
	 * @param version 数据库版本
	 */
	public DBHelperUtil(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
		isSucess=true;
		mContext=context;
		if(db==null){
			db=getReadableDatabase(password);//优先使用这种方式
			//db=getWritableDatabase(password);
		}
	}
	
	/**
	 * 只在第一次创建数据库时调用
	 */
	@Override	
	public void onCreate(SQLiteDatabase db) {
		db.beginTransaction();
		try{			
			db.execSQL(timeTable);	
			//test 正式用时删掉
			db.execSQL(CREATE_TABLE);	
			db.setTransactionSuccessful();
			Log.i("Nursation","create patinfo sucess!");
		}catch (Exception e){
			e.printStackTrace();
		}finally{		
			db.endTransaction();
		}
	}
	/**
	 * 更新数据库版本时调用
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Cursor cursor = db.rawQuery("select * from TableRecord;", null);
		while(cursor.moveToNext()){
			db.execSQL("DROP TABLE "+cursor.getString(1));
		}
		try {
			db.execSQL("DROP TABLE TableRecord;");
			db.execSQL(timeTable);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/*public class TableRecord{
		int _id;
		String name;
		long datetime;
	}*/
	/**
	 * 插入
	 * @param list 要插入的对象集合
	 * @param clazz 该表对应的类
	 * @param type 0正常 1清空表 2删除同group_id的数据
	 * @param group_id 分组标识
	 */
	public <E>  boolean insertData(List<E> list,Class<E> clazz,int type,String group_id) {		
		if(list==null||list.size()<1||clazz==null){	//空数据直接返回
			return false;
		}	
		if(db.inTransaction()){
			return false;
		}
		String table = clazz.getSimpleName();
		if(!isTableExists(table)){	//如果表不存在则新建表
			if(!createTable(clazz)){
				return false;
			}
		}else{
			if(type==1){	//如果要清空 则把表删除重新建
				deleteTable(clazz);
				if(!createTable(clazz)){
					return false;
				}
			}else if(type==2){			//删除以前同groupid的数据
				deleteByGroupId(clazz, group_id);
			}
		}
		
		ContentValues values = null;
		Field[] field =clazz.getFields();//获取public的字段
		System.out.println(field.length);
		db.beginTransaction();
		for(E i:list){			
			if(i!=null){
				
				values = new ContentValues();
				
				for(Field j:field){
					try {
						if(j.get(i)!=null){
							values.put(j.getName(), j.get(i).toString());
						}
					} catch (Exception e) {
						e.printStackTrace();
					} 
				}
				if(group_id!=null && group_id.length()>0){
					values.put("_GROUP_ID", group_id);
				}
				db.insert(table, "_GROUP_ID", values);
			}	
		}
		db.setTransactionSuccessful();
		db.endTransaction();
		return true;
	}
	
	/**
	 * 
	* @Title: insertData 
	* @Description: TODO(带表名的插入方法) 
	* @param @param list
	* @param @param clazz
	* @param @param type
	* @param @param group_id
	* @param @param tablename
	* @param @return    设定文件 
	* @return boolean    返回类型 
	* @author zhangwei
	 */
	public <E>  boolean insertData(List<E> list,Class<E> clazz,int type,String group_id, String tablename) {		
		if(list==null||list.size()<1||clazz==null){	//空数据直接返回
			return false;
		}	
		if(db.inTransaction()){
			return false;
		}
		String table = tablename;
		if(!isTableExists(table)){	//如果表不存在则新建表
			if(!createTable(clazz)){
				return false;
			}
		}else{
			if(type==1){	//如果要清空 则把表删除重新建
				deleteTable(clazz);
				if(!createTable(clazz)){
					return false;
				}
			}else if(type==2){			//删除以前同groupid的数据
				deleteByGroupId(clazz, group_id);
			}
		}
		
		ContentValues values = null;
		Field[] field =clazz.getFields();//获取public的字段
		System.out.println(field.length);
		db.beginTransaction();
		for(E i:list){			
			if(i!=null){
				
				values = new ContentValues();
				
				for(Field j:field){
					try {
						if(j.get(i)!=null){
							values.put(j.getName(), j.get(i).toString());
						}
					} catch (Exception e) {
						e.printStackTrace();
					} 
				}
				if(group_id!=null && group_id.length()>0){
					values.put("_GROUP_ID", group_id);
				}
				db.insert(table, "_GROUP_ID", values);
			}	
		}
		db.setTransactionSuccessful();
		db.endTransaction();
		return true;
	}
	
	public<E> boolean insertData(E data,Class<E> clazz,int type,String group_id){
		if(data==null){
			return false;
		}
		ArrayList<E> list = new ArrayList<E>();
		list.add(data);
		return insertData(list, clazz, type, group_id);
	}
	
	@SuppressWarnings("unchecked")
	public<E> boolean insertData(E data){
		if(data==null){
			return false;
		}	
		Class<E> clazz = (Class<E>) data.getClass();
		ArrayList<E> list = new ArrayList<E>();
		list.add(data);
		return insertData(list, clazz, INSERT_TYPE_DELETE_SAME_GROUPID, null);
	}
	@SuppressWarnings("unchecked")
	public<E> boolean insertData(E data,String group_id){
		if(data==null){
			return false;
		}
		Class<E> clazz = (Class<E>) data.getClass();
		ArrayList<E> list = new ArrayList<E>();
		list.add(data);
		
		return insertData(list, clazz, INSERT_TYPE_DELETE_SAME_GROUPID, group_id);
	}
	public <E> List<E> queryTableByWhere(Class<E> clazz,String where){
		
		if (clazz == null) {
			return null;
		}
		String table = clazz.getSimpleName();
		if(!isTableExists(table)){
			return null;
		}
		ArrayList<E> list = new ArrayList<E>();
		E temp = null;
		int pos=0;
		Field[] field =clazz.getFields();//获取public的字段
		String sql = "select * from "+table+" ";
		if(where!=null && !TextUtils.isEmpty(where)){
			sql +=where;
		}	
		Cursor cursor= db.rawQuery(sql,null);
		if(cursor==null){
			return list;
		}
		while(cursor.moveToNext()){
			try {
				temp = clazz.newInstance();
				for(int i=0;i<field.length;i++){
					pos=cursor.getColumnIndex(field[i].getName());
					if(field[i].getType().getName().equals(java.lang.String.class.getName())){//String字段
						field[i].set(temp, cursor.getString(pos));						
					}else if(field[i].getType().getName().equals(java.lang.Integer.class.getName())){//int 字段
						field[i].set(temp, cursor.getInt(pos));
					}else if(field[i].getType().getName().equals(java.lang.Long.class.getName())){//long 字段
						field[i].set(temp, cursor.getLong(pos));
					}else{
						
					}
				}
				list.add(temp);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		cursor.close();
		return list;
	}
	public Cursor queryTableByGroupId(String table,String group_id){
		if(!isTableExists(table)){
			return null;
		}
		String sql = "select * from "+table;
		if(group_id !=null && group_id.length()>0){
			sql +=" where _GROUP_ID='"+group_id+"';";
		}	
		Cursor cursor= db.rawQuery(sql,null);
		if(cursor==null){
			return null;
		}
		return cursor;
	}
	/**
	 * 整表查询  根据group_id查询  group_id可以为空
	 * @param clazz
	 * @param group_id 分组标识
	 */
	public <E> List<E> queryTableByGroupId(Class<E> clazz,String group_id){
		if (clazz == null) {
			return null;
		}
		String table = clazz.getSimpleName();
		if(!isTableExists(table)){
			return null;
		}
		ArrayList<E> list = new ArrayList<E>();
		E temp = null;
		int pos=0;
		Field[] field =clazz.getFields();//获取public的字段
		String sql = "select * from "+table;
		if(group_id !=null && group_id.length()>0){
			sql +=" where _GROUP_ID='"+group_id+"';";
		}
		Cursor cursor= db.rawQuery(sql,null);
		if(cursor==null){
			return null;
		}
		while(cursor.moveToNext()){
			try {
				temp = clazz.newInstance();
				for(int i=0;i<field.length;i++){
					pos=cursor.getColumnIndex(field[i].getName());
					if(field[i].getType().getName().equals(java.lang.String.class.getName())){//String字段
						//Log.i("DBHelper", "pos="+pos+",value="+cursor.getString(pos));
						field[i].set(temp, cursor.getString(pos));						
					}else if(field[i].getType().getName().equals(java.lang.Integer.class.getName())){//int 字段
						field[i].set(temp, cursor.getInt(pos));
					}else if(field[i].getType().getName().equals(java.lang.Long.class.getName())){//long 字段
						field[i].set(temp, cursor.getLong(pos));
					}else{
						
					}
				}
				list.add(temp);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		cursor.close();
		return list;
	}
	
	/**
	 * 删除表 
	 * @param tableName 要删除的表名
	 */
	public boolean deleteTable(String tableName){		
		try {
			db.execSQL("DROP TABLE "+tableName+";");
			db.execSQL("delete from TableRecord where name='"+tableName+"';");
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}	
	}
	
	/**
	 * 删除表
	 * @param clazz 要删除的表关联的类
	 */
	public <E> boolean deleteTable(Class<E> clazz){	
		return deleteTable(clazz.getSimpleName());
	}
	
	/**
	 * 检查表是否存在
	 * @param tableName 表名
	 * @return
	 */
	public boolean isTableExists(String tableName) {    
	   try {
		   
		   /*if(!CustomApplication.dbHelperView.DBIsConnected()){
				CustomApplication.dbHelperView=new DBHelperUtil(  mContext,
						CustomApplication.DATABASE_PATH+"/"+HttpParamsConstants.dbName, null, 1);
			}*/
		   
		   Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '"+tableName+"'", null);  
		    if(cursor!=null) {  
		        if(cursor.getCount()>0) {   
		            cursor.close();  
		            return true;  
		        }  
		        cursor.close();  
		    }  
	} catch (Exception e) {
		return false;  
	}
	    return false;  
	}
	
	/**
	 * 建表
	 * @param tableName 表名
	 * @param length 字段长 >0 且 <200
	 */
	public <E>boolean createTable(Class<E> clazz){
		if(clazz==null){
			return false;
		}
		String tableName = clazz.getSimpleName();
		Field[] fields = clazz.getFields();
		try {
			String sqlCreate = "CREATE TABLE "+tableName+"(_id integer primary key autoincrement";
			for(Field i:fields){
				sqlCreate += ","+i.getName()+" varchar";
			}
			sqlCreate +=",_GROUP_ID varchar);";
			db.execSQL(sqlCreate);
			Calendar cal = Calendar.getInstance(Locale.CHINA);
			db.execSQL("insert into TableRecord(name,datetime) values('"+tableName+"',"+cal.getTimeInMillis()+")");

			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * 根据groupId删除数据
	 * @param tableName  表名
	 * @param groupId	分组标识
	 */
	public void deleteByGroupId(String tableName,String groupId){
		if(tableName==null||groupId==null){
			return;
		}
		String sql = "delete from "+tableName+" where _GROUP_ID='"+groupId+"'";
		try {
			db.execSQL(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 根据groupId删除数据
	 * @param tableName  表名
	 * @param groupId	分组标识
	 */
	public void deleteByWhere(String tableName,String where){
		if(tableName==null){
			return;
		}
		String sql = "delete from "+tableName;
		if(where !=null ){
			sql +=" where "+where+";";
		}	
		
		try {
			db.execSQL(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 根据groupId删除数据
	 * @param clazz  表关联的类
	 * @param groupId	分组标识
	 */
	public <E> void deleteByGroupId(Class<E> clazz,String groupId){
		if(clazz==null){
			return;
		}
		deleteByGroupId(clazz.getSimpleName(), groupId);
	}
	
	/**
	 * 删除数据
	 * @param clazz 目的表关联的类
	 * @param whereClause 查询条件
	 * @param whereArgs  查询条件中占位符对应的参数
	 */
	public<E> void deleteData(Class<E> clazz,String whereClause,String[] whereArgs){
		if(clazz==null){
			return;
		}
		db.delete(clazz.getSimpleName(), whereClause, whereArgs);
	}
	
	/**
	 * 删除数据
	 * @param table 表名
	 * @param whereClause 查询条件
	 * @param whereArgs  查询条件中占位符对应的参数
	 */
	public void deleteData(String table,String whereClause,String[] whereArgs){
		db.delete(table, whereClause, whereArgs);
	}
	/**
	 * 更新数据
	 * @param table  表名
	 * @param values 要改动的键值对
	 * @param whereClause	查询条件
	 * @param whereArgs	查询条件中占位符对应的参数
	 */
	public int update(String table,ContentValues values,String whereClause,String[] whereArgs){
		return db.update(table, values, whereClause, whereArgs);
	}
	
	/**
	 * 更新数据
	 * @param clazz  目的表关联的类
	 * @param values 要改动的键值对
	 * @param whereClause	查询条件
	 * @param whereArgs	查询条件中占位符对应的参数
	 */
	public<E> int update(Class<E> clazz,ContentValues values,String whereClause,String[] whereArgs){
		if(clazz==null){
			return 0;
		}
		return db.update(clazz.getSimpleName(), values, whereClause, whereArgs);
	}
	
	/**
	 * 执行sql语句
	 * @param sql语句 
	 */
	public void execSQL(String sql){
		db.execSQL(sql);
	}
	/**
	 * 根据sql语句查询表
	 * @param clazz  要返回的数据集合对应的对象类
	 * @param sql	sql语句
	 * @return clazz的集合
	 */
	public <E> ArrayList<E> quary(Class<E> clazz,String sql){
		ArrayList<E> list = new ArrayList<E>();
		Cursor cursor = null;
		try {
			cursor= db.rawQuery(sql,null);
		} catch (Exception e1) {
			return null;
		}
		if(cursor==null){
			return null;
		}
		E temp = null;
		Field[] field =clazz.getFields();//获取public的字段
		int pos=0;
		while(cursor.moveToNext()){
			try {
				temp = clazz.newInstance();
				for(int i=0;i<field.length;i++){
					pos=cursor.getColumnIndex(field[i].getName());
					if(pos==-1){
						continue;
					}
					if(field[i].getType().getName().equals(java.lang.String.class.getName())){//String字段
						field[i].set(temp, cursor.getString(pos));						
					}else if(field[i].getType().getName().equals(java.lang.Integer.class.getName())){//int 字段
						field[i].set(temp, cursor.getInt(pos));
					}else if(field[i].getType().getName().equals(java.lang.Long.class.getName())){//long 字段
						field[i].set(temp, cursor.getLong(pos));
					}else {
					}
				}
				list.add(temp);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		cursor.close();
		return list;
	}
	
	/**
	 * 查询
	 * @param clazz 类名 要返回的数据类型
	 * @param where 查询条件 为空时即为整表查询
	 * @return
	 */
	public <E> ArrayList<E> queryTable(Class<E> clazz,String where){
		String table = clazz.getSimpleName();
		if(!isTableExists(table)){
			return null;
		}
		String sql = "select * from "+table;
		if(where !=null && where.length()>0){
			sql +=" where "+where+";";
		}	
		return quary(clazz, sql);
	}
	
	/**
	 * 查询pda配置
	 * @param clazz 类名 要返回的数据类型
	 * @param where 查询条件 为空时即为整表查询
	 * @return
	 */
	public <E> ArrayList<E> queryTable(Class<E> clazz,String where, String tableName){
		String table = tableName;
		if(!isTableExists(table)){
			return null;
		}
		String sql = "select * from "+table;
		if(where !=null && where.length()>0){
			sql +=" where "+where+";";
		}	
		return quary(clazz, sql);
	}
	
	
	/**
	 * 
	 * @param mhandler   		接收handler
	 * @param what		    	接收的msg 的what值
	 * @param reportName		评估单名称
	 * @param version			评估单版本
	 *//*
	public void queryViewConfig(Handler mhandler, int what, String reportName, String version){
		ArrayList<ViewConfigParentItem> mItemList2=new ArrayList<ViewConfigParentItem>();
		if (CustomApplication.dbHelperView != null) {
			if (CustomApplication.dbHelperView.isTableExists("PDA_REPORT_WIDGET")) {
				mItemList2 = CustomApplication.dbHelperView.queryTable(ViewConfigParentItem.class,
						" REPORT_NAME='" + reportName + "'" ,"PDA_REPORT_WIDGET");
			}
		}
		Message msg=new Message();
		msg.obj=mItemList2;
		msg.what=what;
		mhandler.sendMessage(msg);
	}
	
	*//**
	 * 
	* @Title: queryReportConfig 
	* @Description: TODO(从本地数据库查询动态配置的表单) 
	* @return ArrayList<ReportEntity>    返回类型 
	* @throws 
	* @author wing
	* @date 2015-9-1 下午2:01:40
	 *//*
	public ArrayList<ReportEntity> queryReportConfig(String wardName,String parentId){
 		ArrayList<ReportEntity> ret = new ArrayList<ReportEntity>();
 		if (CustomApplication.dbHelperView != null) {
 			if (CustomApplication.dbHelperView.isTableExists("PDA_REPORT")) {
				String where = " IS_HIDE <> '1'  AND PARENT_ID='"+parentId+"' ";
				ret = CustomApplication.dbHelperView.queryTable(ReportEntity.class,
						where,"PDA_REPORT");
 			}
 		}
		return ret;
	}
	
	*//**
	 * 
	* @Title: queryReportConfig 
	* @Description: TODO(将所有表单查询出来的方法) 
	* @param @param wardName
	* @param @return    设定文件 
	* @return ArrayList<ReportEntity>    返回类型 
	* @author zhangwei
	 *//*
	public ArrayList<ReportEntity> queryReportConfig(String wardName){
 		ArrayList<ReportEntity> ret = new ArrayList<ReportEntity>();
 		if (CustomApplication.dbHelperView != null) {
 			if (CustomApplication.dbHelperView.isTableExists("PDA_REPORT")) {
				String where = " IS_HIDE <> '1'";
				ret = CustomApplication.dbHelperView.queryTable(ReportEntity.class,
						where,"PDA_REPORT");
 			}
 		}
		return ret;
	}*/
	
	
	/**
	 * 
	* @Title: DBIsConnected 
	* @Description:(判断数据库是否打开) 
	* @return boolean    返回类型 
	* @throws 
	* @author wing
	* @date 2015-9-9 下午1:27:39
	 */
	public boolean DBIsConnected(){
		if(null!= db){
			return db.isOpen();
		}else{
			return false;
		}
	}
	
	/** 
	 * <p><b>Description:</b> 返回当前连接的SQLiteDatabase实例</p>
	 * <p><b>Title:</b> getSqLiteDatabase </p>
	 * @return  SQLiteDatabase instance
	 */
	public SQLiteDatabase getSqLiteDatabase(){
		return (db!=null)?db:getReadableDatabase(password);
	}
}
