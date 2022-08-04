/*

  Different SQL Operators:::    = , <, >, >=, <=, <>, !=, BETWEEN, ORDER BY, IN, NOT IN, LIKE, ALIASE, DISTINCT, LIMIT, CASE:
  Comparison Operators:        =, <>, != , >, <, >=, <=
  Arithmetic Operators:        +, -, *, /, %
  Logical Operators:           AND, OR, NOT, IN, BETWEEN, LIKE etc.

  Constrains:
    - CHECK
    - NOT NULL,
    - UNIQUE
    - PRIMARY KEY     => on one or few columns
    - FOREIGN KEY

There are two way of writing select queries with a join:
They both are ok to use

    1. select t1.column, t2.column
       from table_one as t1
       join table_two as t2 on t1.id = t2.id

    2. select t1.column, t2.column
       from table_one as t1, table_two as t2
       where t1.id = t2.id

*/


-- Comparison Operators
select * from subjects where subject_name = 'Mathematics'; -- Fetch all records where subject name is Mathematics.
select * from subjects where subject_name <> 'Mathematics'; -- Fetch all records where subject name is not Mathematics.
select * from subjects where subject_name != 'Mathematics'; -- same as above. Both "<>" and "!=" are NOT EQUAL TO operator in SQL.
select * from staff_salary where salary > 10000; -- All records where salary is greater than 10000.
select * from staff_salary where salary < 10000; -- All records where salary is less than 10000.

select *
from staff_salary
where salary < 10000
order by salary; -- All records where salary is less than 10000 and the output is sorted in ascending order of salary.

select *
from staff_salary
where salary < 10000
order by salary desc; -- All records where salary is less than 10000 and the output is sorted in descending order of salary.

select *
from staff_salary
where salary >= 10000; -- All records where salary is greater than or equal to 10000.

select *
from staff_salary
where salary <= 10000; -- All records where salary is less than or equal to 10000.



                                                    -- Logical Operators


select *
from staff_salary
where salary between 5000 and 10000; -- Fetch all records where salary is between 5000 and 10000.

select *
from subjects
where subject_name IN ('Mathematics', 'Science', 'Arts'); -- All records where subjects is either Mathematics, Science or Arts.

select *
from subjects
where subject_name NOT IN ('Mathematics', 'Science', 'Arts'); -- All records where subjects is not Mathematics, Science or Arts.

select *
from subjects
where subject_name LIKE 'Computer%'; -- Fetch records where subject name has Computer as prefixed. % matches all characters.

select *
from subjects
where subject_name NOT LIKE 'Computer%'; -- Fetch records where subject name does not have Computer as prefixed. % matches all characters.

select *
from staff
where age > 50
	and gender = 'F'; -- Fetch records where staff is female and is over 50 years of age. AND operator fetches result only if the condition mentioned both on left side and right side of AND operator holds true. In OR operator, atleast any one of the conditions needs to hold true to fetch result.

select *
from staff
where first_name LIKE 'A%'
	and last_name LIKE 'S%'; -- Fetch record where first name of staff starts with "A" AND last name starts with "S".

select *
from staff
where first_name LIKE 'A%'
	or last_name LIKE 'S%'; -- Fetch record where first name of staff starts with "A" OR last name starts with "S". Meaning either the first name or the last name condition needs to match for query to return data.

select *
from staff
where
  (first_name LIKE 'A%' or last_name LIKE 'S%')
	and age > 50; -- Fetch record where staff is over 50 years of age AND has his first name starting with "A" OR his last name starting with "S".




                                                        -- Arithmetic Operators



select (5 + 2) as addition; -- Sum of two numbers. PostgreSQL does not need FROM clause to execute such queries.
select (5-2) as subtract; -- Oracle & MySQL equivalent query would be -->  select (5+2) as Addition FROM DUAL; --> Where dual is a dummy table.
select (5 * 2) as multiply;
select (5 / 2) as divide; -- Divides 2 numbers and returns whole number.
select (5 % 2) as modulus; -- Divides 2 numbers and returns the remainder

select staff_type from staff ; -- Returns lot of duplicate data.
select distinct staff_type from staff ; -- Returns unique values only.
select staff_type from staff limit 5; -- Fetches only the first 5 records from the result.


select * from staff_view;



                                                    -- CASE statement



(IF 1 then print True ; IF 0 then print FALSE ; ELSE print -1)

-- multiply case is allowed in select statement but each case for each column
select age, gender, staff_id,
  case
    when age > 50 then 'age is more than 50'
  end as range,
  case
    when gender = 'F' then 'M'
  end as gender_edit
from staff;

select staff_id,
	salary,
	case
	  when salary >= 10000 then 'High Salary'
	  when salary between 5000 and 10000 then 'Average Salary'
	  when salary < 5000 then 'Too Low'
	end as range
from staff_salary
order by 2 desc;



                                                      -- TO_CHAR / TO_DATE:


select *
from students
where to_char(dob, 'YYYY') = '2014';

select to_char(dob, 'DD'), to_char(dob, 'MM'), to_char(dob, 'YYYY')
from students
where to_char(dob, 'MM') = '02';

select *
from students
where dob = to_date('13-JAN-2014', 'DD-MON-YYYY');



                                                      -- JOINS


--(Two ways to write SQL queries):
-- #1. Using JOIN keyword between tables in FROM clause.

select t1.column1 as c1,
	t1.column2 c2,
	t2.column3 as c3 -- C1, C2, C3 are aliase to the column

from table1 t1
join table2 as t2 on t1.c1 = t2.c1
and t1.c2 = t2.c2; -- T1, T2 are aliases for table names.

-- #2. Using comma "," between tables in FROM clause.

select t1.column1 as c1,
	t1.column2 as c2,
	t2.column3 c3
from table1 as t1,
	table2 as t2
where t1.c1 = t2.c1
	and t1.c2 = t2.c2;


-- Fetch all the class name where Music is thought as a subject.
select class_name
from subjects sub
join classes cls on sub.subject_id = cls.subject_id
where subject_name = 'Music';


-- Fetch the full name of all staff who teach Mathematics.
select distinct (stf.first_name || ' ' || stf.last_name) as full_name --, CLS.CLASS_NAME
from subjects sub
join classes cls on cls.subject_id = sub.subject_id
join staff stf on cls.teacher_id = stf.staff_id
where sub.subject_name = 'Mathematics';


-- Fetch all staff who teach grade 8, 9, 10 and also fetch all the non-teaching staff
-- UNION can be used to merge two differnt queries. UNION returns always unique records so any duplicate data while merging these queries will be eliminated.
-- UNION ALL displays all records including the duplicate records.
-- When using both UNION, UNION ALL operators, rememeber that noo of columns and their data type must match among the different queries.

select stf.staff_type,
	(stf.first_name || ' ' || stf.last_name) as full_name,
	stf.age,
	(case
		 when stf.gender = 'M' then 'Male'
		 when stf.gender = 'F' then 'Female'
	 end) as gender,
	stf.join_date
from staff stf
join classes cls on stf.staff_id = cls.teacher_id
where stf.staff_type = 'Teaching'
	and cls.class_name IN ('Grade 8','Grade 9','Grade 10')
union all
select staff_type,
	(first_name || ' ' || last_name) as full_name,
	age,
	(case
	   when gender = 'M' then 'Male'
	   when gender = 'F' then 'Female'
	 end) as gender,
	join_date
from staff
where staff_type = 'Non-Teaching';

-- Select count of students on each class
select sc.class_id, count(*) as "amount_of_students"
from student_classes sc
group by sc.class_id
order by sc.class_id;

-- Select count of students on each class where amount of them is greater than 100
select sc.class_id, count(*) as "amount_of_students"
from student_classes sc
group by sc.class_id
having count(*) > 100
order by sc.class_id;

-- Parents with more than 1 kid in school.
select parent_id, count(*) as "count_of_kids"
from student_parent sp
group by parent_id
having count(1) > 1;




                            -- SUBQUERY: Query written inside a query is called subquery



-- Fetch the details of parents having more than 1 kids going to this school. Also display student details.
select (p.first_name || ' ' || p.last_name)                                    as parent_name,
	(s.first_name || ' ' || s.last_name)                                         as student_name,
	s.age                                                                        as student_age,
	s.gender                                                                     as student_gender,
	(adr.street || ', ' || adr.city || ', ' || adr.state || ', ' || adr.country) as address
from parents p
join student_parent sp on p.id = sp.parent_id
join students s on s.id = sp.student_id
join address adr on p.address_id = adr.address_id
where p.id IN
		(select parent_id
			from student_parent sp
			group by parent_id
			having count(1) > 1)
order by 1;

-- Staff details who’s salary is less than 5000
select staff_type, first_name, last_name
from staff
where staff_id IN
		(select staff_id
			from staff_salary
			where salary < 5000);




                               --  Aggregate Functions (AVG, MIN, MAX, SUM, COUNT):



-- AVG: Calculates the average of the given values.
select avg(ss.salary)::numeric(10, 2) as avg_salary
from staff_salary ss
join staff stf on stf.staff_id = ss.staff_id
where stf.staff_type = 'Teaching';

select stf.staff_type, avg(ss.salary)::numeric(10,2) as avg_salary
from staff_salary ss
join staff stf on stf.staff_id = ss.staff_id
group by stf.staff_type;



/* Note:
  '::NUMERIC' is a cast operator which is used to convert values from one data type to another.
  In the above query we use it display numeric value more cleanly by restricting the decimal point to only 2.
  Here 10 is precision which is the total no of digits allowed.
  2 is the scale which is the digits after decimal point.
*/

-- SUM: Calculates the total sum of all values in the given column.
select stf.staff_type, sum(ss.salary)::numeric(10, 2) as avg_salary
from staff_salary ss
join staff stf on stf.staff_id = ss.staff_id
group by stf.staff_type;

-- MIN: Returns the record with minimun value in the given column.
select stf.staff_type, min(ss.salary)::numeric(10,2) as avg_salary
from staff_salary ss
join staff stf on stf.staff_id = ss.staff_id
group by stf.staff_type;

-- MAX: Returns the record with maximum value in the given column.
select stf.staff_type, max(ss.salary)::numeric(10,2) as avg_salary
from staff_salary ss
join staff stf on stf.staff_id = ss.staff_id
group by stf.staff_type;



/*
SQL Joins: There are several types of JOIN but we look at the most commonly used:
1) Inner Join
    - Inner joins fetches records when there are matching values in both tables.
2) Outer Join
    - Left Outer Join
        - Left join fetches all records from left table and the matching records from right table.
        - The count of the query will be the count of the Left table.
        - Columns which are fetched from right table and do not have a match will be passed as NULL.
    - Right Outer Join
        - Right join fetches all records from right table and the matching records from left table.
        - The count of the query will be the count of the right table.
        - Columns which are fetched from left table and do not have a match will be passed as NULL.
    - Full Outer Join
        - Full join always return the matching and non-matching records from both left and right table.
*/

-- Inner Join: 21 records returned – Inner join always fetches only the matching records present in both right and left table.
-- Inner Join can be represented as either "JOIN" or as "INNER JOIN". Both are correct and mean the same.
select count(1)
from staff stf
join staff_salary ss on ss.staff_id = stf.staff_id
order by 1;

select distinct (stf.first_name || ' ' || stf.last_name) as full_name, ss.salary
from staff stf
join staff_salary ss on ss.staff_id = stf.staff_id
order by 2;

-- 23 records – 23 records present in left table.
-- All records from LEFT table with be fetched irrespective of whether there is matching record in the RIGHT table.
select count(1)
from staff stf
left join staff_salary ss on ss.staff_id = stf.staff_id
order by 1;

select distinct (stf.first_name || ' ' || stf.last_name) as full_name,ss.salary
from staff stf
left join staff_salary ss on ss.staff_id = stf.staff_id
order by 2;

-- 24 records – 24 records in right table.
-- All records from RIGHT table with be fetched irrespective of whether there is matching record in the LEFT table.
select count(1)
from staff stf
right join staff_salary ss on ss.staff_id = stf.staff_id
order by 1;

select distinct (stf.first_name || ' ' || stf.last_name) as full_name, ss.salary
from staff stf
right join staff_salary ss on ss.staff_id = stf.staff_id
order by 1;

-- 26 records – all records from both tables. 21 matching records + 2 records from left + 3 from right table.
-- All records from both LEFT and RIGHT table with be fetched irrespective of whether there is matching record in both these tables.
select count(1)
from staff stf
full outer join staff_salary ss on ss.staff_id = stf.staff_id
order by 1;

select distinct (stf.first_name || ' ' || stf.last_name) as full_name, ss.salary
from staff stf
full outer join staff_salary ss on ss.staff_id = stf.staff_id
order by 1,2;



/*
                                                       VIEWS




  cases:
    1. create view
    2. alter table by adding new column
    3. view does not contain this new column. View should be recreated only then new column will appear.

  - View can be updated in case it was created based on only one table.
*/

create view some_view
  as
  select * from staff;


select * from staff_salary;
select avg(salary) from staff_salary;



/*
                                                    SUB-QUERY

SUB-QUERY types:
  1. Scalar subquery
  2. Multiply row
  3. Correlated

-- SCALAR SUBQUERY
        if return 1 row and 1 column
   Find the employees who's salary is greatest than avg salary of all employee
    1. find avg              select avg(salary) from staff_salary
    2. find employees        select * from staff_salary where salary ...
*/
select * from staff_salary where salary > (select avg(salary) from staff_salary) order by salary desc;

select *
from staff_salary ss
join (select avg(salary) as salary from staff_salary) avg_sal
on ss.salary > avg_sal.salary;


/*
  MULTIPLY ROW SUBQUERY
      if return  1 row and few columns
                few row and few columns
  Find the employees who's salary is greatest than avg salary of all employee
    1. find avg              (select avg(salary) from staff_salary)
    2. find employees        select * from staff_salary where salary ...
  Question: Find the employees who earn the highest salary in each department
    - group all employees by department and find max salary by using aggregation function
    - select all employees with a `where close`
*/

select *
from employee emp
where (emp.department, emp.salary) in (
  select dep_name, max(salary)
  from employees
  group by department;)



/*
  MULTIPLY ROW SUBQUERY
      if return:  few row and 1 columns
  Find department who has no employees
    employee table
          emp_name | dep_name | ....
    department table
          dep_name | ....
*/

select *
from department dep
where dep.dep_name not in (select dep_name from employee)



/*
  CORRELATED SUBQUERY
  Sub query which is related to the outer query
  Find the employee in each department who earn more than average salary in that department

  - average salary for department
  - Find the employee more than result above

  employee table
          emp_name | dep_name | salary | ...
    department table
          dep_name | ....
*/

-- one way to solve this query
select *
from employee emp
join (select dep_name, avg(salary)
      from employee emp_in
      group by emp_in.dep_name) as res
on emp.dep_name=res.dep_name
where  emp.salary > res.salary;

-- another way to solve this query (basically which demonstrate the way described in CORRELATED SUBQUERY)
-- so here we do have `e1.dep_name = e2.dep_name`
-- so postgres will execute query `e1` with a result of 21 rows and then for each row it will execute `e2` query scope
-- and that is way how it works.
select *
from employee e1
where emp1.salary > (select avg(salary)
                     from employee e2
                     where e1.dep_name = e2.dep_name)


-- Question: Find departments who has no employees
select *
from department d1
where not exists (select 1 from employee emp where d1.dep_name != emp.dep_name)



/*
                                                  Subquery inside subquery
*/

-- Find stores who's sales better than the average sales across stores

with sales as
  (select store_name, sum(price) as total_sales
    from sales
     group by store_name)
select *
from sales
join (select avg(total_sales) as sales from sales x) avg_sales
on sales.total.price > avg_sales.sales


-- WITH CLOUSE
     -- name -----     -- query itself ------
with students_with as (select * from students)
select * from students_with -- using that query here


/*
  Subquery can be used in:
    -   SELECT - which is not reccomended
    -   FROM
    -   WHERE
    -   HAVING
*/
-- subquery in  Having example:
-- Question: Find the store who sold more units than the average units sold by all stores

select store_name, sum(quantity)
from sales
group by store_name
having sum(quantity) > (select avg(quantity) from sales);



/*
                                                            INSERT
*/


-- Question: Insert employees into the history table and make sure there are no duplicates
insert into employee_history
select *
from eployee
join department
on employee.dep_name=deparrtment.dep_name
where not exists (select 1 from employee_history where employee.id=employee_history.id)


/*
                                                          UPDATE
*/
-- Question: Give 10% increment to all employee in Bangalor location based on the maximum salary earned by
-- an employee in each departament. Only consider  employees in employee_history table

update employeee e
set salary = (select max(salary) + (max(salary * 0.1))
              from employee_history eh
              where eh.dep_name = e.dep_name)
where e.dep_name in (select dep_name from department where location = 'Bangalor') and
      e.id in (select id from employee_history);



/*
                                            WINDOW FUNCTION


*/


select * from employee;

select *, max(salary) over(partition by dept_name) as max_salary
from employee e;

/*  Row_number()  */
select *,
row_number() over(partition by dept_name) as rm
from employee


-- Find the first 2 employee who has joined the company for each department
select *
from (
  select *,
  row_number() over(partition by dept_name order by e.emp_id) as rm
  from employee e
) x
where x.rm < 3;

/*  rank()  */

-- Find the top 3 employees in each department earning the max salary
select *
from (
  select *,
  rank() over(partition by dept_name order by salary desc) as asd
  from employee e) x
where x.asd < 4 and ;

/*  dense_rank()

  The deference between rank() and dense_rank() is that in case of similar values heppest `dense_rank` does not skip
  the number. Just run to see what it means
*/
select *
from (
    select *,
      rank()       over(partition by dept_name order by salary desc) as rank,
      dense_rank() over(partition by dept_name order by salary desc) as dense_rank,
      row_number() over(partition by dept_name order by salary desc) as row_number
    from employee e) x
where x.row_number < 4;


/*  lag()   - looks at the previous record
    lead()  - looks at the next record
    Question: If salary of an employee is higher or lower or equal to the previous employee
*/
select *,
lag(salary, 1 /*row number to look at*/, 0 /*default number in case of first or last record*/) over(partition by dept_name order by salary desc) as foo
lag(salary, 1 /*row number to look at*/, 0 /*default number in case of first or last record*/) over(partition by dept_name order by salary desc) as foo
from employee;






