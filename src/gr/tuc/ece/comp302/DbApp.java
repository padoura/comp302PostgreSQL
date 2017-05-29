package gr.tuc.ece.comp302;
import java.awt.Choice;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.InputMismatchException;
import java.util.Random;
import java.util.Scanner;


public class DbApp {
	Connection conn;

	public DbApp() {
		conn = null;
	}
	
	
	public void dbConnect (String ip, int port, String database, String username, String password) {
		try {
			// Check if postgres driver is loaded
     		Class.forName("org.postgresql.Driver");
     		// Establish connection with the database
     		conn = DriverManager.getConnection("jdbc:postgresql://"+ip+":"+port+"/"+database,username,password);
     		System.out.println("Connection Established!");
     		// Disable autocommit.
     		conn.setAutoCommit(false);
     		System.out.println("Autocommit turned off!");
     		
     		
		} catch(Exception e) {
            e.printStackTrace();
		}
	}
	
	public void db_commit() {
		try {
			// Commit all changes
			conn.commit();
			System.out.println("Changes committed!");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void db_abort() {
		try {
			// Rollback all changes
			conn.rollback();
			System.out.println("Uncommitted changes cancelled!");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void waitForEnter() {
		Scanner scn = new Scanner(System.in);
		System.out.println("Press Enter to continue...");
		scn.nextLine();
	}
	
	/*
	 * Insert new thesis using the stored procedure of Phase A Question 1.4
	 * All changes made by this procedure are committed automatically
	 */
	public void insertDiploma(){
		Scanner input = new Scanner(System.in);
		CallableStatement storedCall;
		try {
			storedCall = conn.prepareCall("{call insert_thesis_q1p4(?,?,?,?,?)}");
			
			
			System.out.println("Enter AMKA of supervisor:");
			String supervisor_amka = input.nextLine();
			
			System.out.println("Enter AMKA of first committee member:");
			String prof1_amka = input.nextLine();
			
			System.out.println("Enter AMKA of second committee member:");
			String prof2_amka = input.nextLine();
			
			System.out.println("Enter AM of student:");
			String student_am = input.nextLine();
			
			System.out.println("Enter thesis title:");
			String thesis_title = input.nextLine();
			
			storedCall.setString(1, supervisor_amka);
			storedCall.setString(2, prof1_amka);
			storedCall.setString(3, prof2_amka);
			storedCall.setString(4, student_am);
			storedCall.setString(5, thesis_title);
			storedCall.executeUpdate();
			storedCall.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
		} finally {
			waitForEnter();
		}
	}
	
	/*
	 * Update exam and/or lab grade of a particular course and student.
	 */
	public void changeGrade() {
		Scanner input = new Scanner(System.in);
		PreparedStatement statement;
		try {
			String query = "UPDATE	\"Register\" "
					+ "SET	exam_grade = ?, lab_grade = ? "
					+ "WHERE	amka = (SELECT	amka FROM \"Student\" WHERE am = ?) AND course_code = ? AND serial_number = ?";
			
			statement = conn.prepareStatement(query);
			
			
			System.out.println("Enter AM of student:");
			String student_am = input.nextLine();
			
			System.out.println("Enter course code:");
			String course_code = input.nextLine();
			
			System.out.print("Enter course serial number. ");
			int serial_number = 0;
			while(serial_number <= 0){
				System.out.println("Please enter a positive integer:");
				while(true){
					try{
						serial_number = input.nextInt();
						input.nextLine(); //discard rest of line
						break;
					}catch (InputMismatchException e){
						System.out.println("Invalid input! Please enter a positive integer:");
					}
				}
			}
			
			

			
			System.out.println("Enter new exam grade:");
			int exam_grade;
			while(true){
				try{
					exam_grade = input.nextInt();
					input.nextLine();
					break;
				}catch (InputMismatchException e){
					System.out.println("Invalid input! Please enter an integer from 0 to 10!");
				}
			}

			System.out.println("Enter new lab grade:");
			int lab_grade;
			while(true){
				try{
					lab_grade = input.nextInt();
					input.nextLine();
					break;
				}catch (InputMismatchException e){
					System.out.println("Invalid input! Please enter an integer from 0 to 10!");
				}
			}
			
			statement.setInt(1, exam_grade);
			statement.setInt(2, lab_grade);
			statement.setString(3, student_am);
			statement.setString(4, course_code);
			statement.setInt(5, serial_number);
			int rowCount = statement.executeUpdate();
			System.out.println(rowCount + " row(s) updated! Commit to finalize changes!");
		} catch (SQLException e1) {
			e1.printStackTrace();
		} finally {
			waitForEnter();
		}
	}

	/*
	 * Print exam, lab and final grade for a particular course and student.
	 * Registration status is also printed.
	 */
	public void showGrade() {
		Scanner input = new Scanner(System.in);
		PreparedStatement statement;
		try {
			String query = "SELECT	exam_grade, lab_grade, final_grade, register_status "
							+ "FROM	\"Register\""
							+ "WHERE	amka = (SELECT	amka FROM \"Student\" WHERE am = ?) "
								+ "AND serial_number = "
								+ "(SELECT max(serial_number) FROM \"Register\" "
								+ "WHERE amka = (SELECT amka FROM \"Student\" WHERE am = ?) AND course_code = ?)";
			
			statement = conn.prepareStatement(query);
			
			
			System.out.println("Enter AM of student:");
			String student_am = input.nextLine();
			
			System.out.println("Enter course code:");
			String course_code = input.nextLine();
			
			statement.setString(1, student_am);
			statement.setString(2, student_am);
			statement.setString(3, course_code);
			ResultSet result = statement.executeQuery();
			if (result.next()){
				System.out.println("Exam grade: " + result.getDouble(1) + " Lab grade: " + result.getDouble(2) + " Final grade: " + result.getDouble(3));
				System.out.println("Registration status: " + result.getString(4));
			}
			else	
				System.out.println("Student " + student_am + " has not been registered to course " + course_code + " or student/course doesn't exist!");
		} catch (SQLException e1) {
			e1.printStackTrace();
		} finally {
			waitForEnter();
		}
	}
	
	/*
	 * It prints a list of grades for all passed or failed courses. Only the last registration of 
	 * each course is printed, along with its corresponding semester.
	 */
	public void showGradeList() {
		Scanner input = new Scanner(System.in);
		PreparedStatement statement;
		try {
			String query = "SELECT	course_code, course_title, semester_id, final_grade "
						+ "FROM	(SELECT	course_code, course_title, final_grade, serial_number "
						+ "FROM	(	(SELECT	final_grade, course_code, serial_number "
						+ "FROM	\"Register\" "
						+ "WHERE amka = "
						+ "(SELECT	amka FROM \"Student\" "
						+ "WHERE am = ?) AND (register_status = 'pass' OR register_status = 'fail')"
						+ ")AS all_completed JOIN("
						+ "SELECT	max(serial_number) AS serial_number, course_code "
						+ "FROM	\"Register\""
						+ "WHERE	amka =	(SELECT	amka FROM \"Student\" "
						+ "WHERE am = ?) AND (register_status = 'pass' OR register_status = 'fail') "
						+ "GROUP BY course_code"
						+ ")AS last_completed USING(serial_number, course_code)"
						+ ") AS code_grade JOIN \"Course\" USING(course_code)"
								+ ") AS title_grade JOIN \"CourseRun\" USING(course_code, serial_number)"
										+ " ORDER BY semester_id;";
			
			statement = conn.prepareStatement(query);
			
			
			System.out.println("Enter AM of student:");
			String student_am = input.nextLine();
			
			statement.setString(1, student_am);
			statement.setString(2, student_am);
			ResultSet result = statement.executeQuery();
			DecimalFormat dformat = new DecimalFormat("0.00"); // final grade with 2 decimal points
			System.out.println("Course Code  Course Title \t\t\t\t\t\t\t\t\t\t       Semester  Final Grade");
			while (result.next()){
				System.out.println(result.getString(1) + "  " + result.getString(2) + "  " + result.getString(3) + "  " + dformat.format(result.getDouble(4)));
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		} finally {
			waitForEnter();
		}
	}
	
	/*
	 * It prints a list of all people whose surnames begin with the given string, along with their
	 * academic status (Professor, Labstaff, Student).
	 */
	
	public void searchPerson() {
		Scanner input = new Scanner(System.in);
		PreparedStatement statement;
		try {
			String query = "SELECT	surname, name, 'Student' "
							+ "FROM	\"Student\" "
							+ "WHERE	surname LIKE ? "
							+ "UNION "
							+ "SELECT	surname, name, 'Professor' "
							+ "FROM	\"Professor\" "
							+ "WHERE	surname LIKE ? "
							+ "UNION "
							+ "SELECT	surname, name, 'Labstaff' "
							+ "FROM \"Labstaff\" "
							+ "WHERE	surname LIKE ?;";
			
			statement = conn.prepareStatement(query);
			
			
			System.out.println("Enter initial part of surname:");
			String initSurname = input.nextLine().concat("%");
			
			statement.setString(1, initSurname);
			statement.setString(2, initSurname);
			statement.setString(3, initSurname);
			ResultSet result = statement.executeQuery();
			System.out.println("Surname \t\t\t\t\t    Name \t\t\t   Status");
			while (result.next()){
				System.out.println(result.getString(1) + "  " + result.getString(2) + "  " + result.getString(3));
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		} finally {
			waitForEnter();
		}
	}
	
	
	
	public void closeConnection(){
		try {
			System.out.println("Closing connection...");
			db_abort();
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	
	public int askConnection() {
		Scanner menuScanner = new Scanner(System.in);
		int choice = -1;
		System.out.println("Welcome!");
		System.out.println("Would you like to connect to database? (0=no/1=yes)");
		while(choice != 1 && choice != 0){
			try{
				choice = menuScanner.nextInt();
				menuScanner.nextLine(); // throw line leftover
				if (choice == 1){
					dbConnect("localhost",5432,"plh302_project_PhaseB", "postgres", "test");
				}
			}
			catch (InputMismatchException e) {
				System.out.println("Invalid input! Please type '1' for yes or '0' for no.");
				menuScanner.nextLine();
			}
		}
		return choice;
	}
	
	public void mainMenu() {
		Scanner menuScanner = new Scanner(System.in);
		int choice = -1;
		
		while(choice != 0){
			try{
				printMainMenu();
				choice = menuScanner.nextInt();
				menuScanner.nextLine();
				switch (choice) {
				case 1:	insertDiploma();
						break;
				case 2: showGrade();
						break;
				case 3: changeGrade();
						break;
				case 4: searchPerson();
						break;
				case 5: showGradeList();
						break;
				case 6:	db_commit();
						break;
				case 7: db_abort();
						break;
				case 0: closeConnection();
						break;
				default: System.out.println("Choice out of range! Please type from 0 to 7!");
				}
			}
			catch (InputMismatchException e) {
				System.out.println("Invalid input! Please type a number from 0 to 7!");
				menuScanner.nextLine();
			}
		}
	}


	private void printMainMenu() {
		System.out.println("Please choose one of the following options:");
		System.out.println("(1) Insert a new thesis");
		System.out.println("(2) Print course grade of a student");
		System.out.println("(3) Update course grade of a student");
		System.out.println("(4) Search person");
		System.out.println("(5) Print grade list of a student");
		System.out.println("(6) Commit transaction");
		System.out.println("(7) Abort transaction");
		System.out.println("(0) Exit (any uncommitted transactions will be aborted!)");
	}
	
	



}
