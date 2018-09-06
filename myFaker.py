#because that library file named "faker", so we cannot make our file name as "faker"
#because this is quite similar to java. the file name is same as your module name.
#here it wants to include the "faker" library header. but your own file name is faker. 
#so it will try to find the class "Factory" in this file. of course it cannot find it.
#from faker import Factory  # compare with c, here it means: from "filename faker", include "class Factory"

from faker import Factory
from math import floor

# for doing a breadth-first generation of employees
from queue import Queue # note, for Python 3, the module is now queue
                        # though the class is still Queue

# functions to generate random values
from random import expovariate, normalvariate, choice, randint, shuffle

# Constants for Company model
SSN_LEN = 9
MIN_SALARY = 40000
MAX_SALARY = 1000000
HQ = 1 # dept no of Headquarters department
DEPT_NAMES = ('Headquarters',
              'Administration',
              'Development',
              'Facilities',
              'Human Resources',
              'Legal',
              'Marketing',
              'Research',
              'Sales',
              'Service'
              )

# Constants to manage number of records to generate
NUM_DEPTS = 10
MIN_SUPERVISEES = 5
MAX_SUPERVISEES = 20
PERCENT_SUPERVISORS = 8  # % of employees who are chosen as supervisors
DEPENDENTS_PER_EMPLOYEE = 2
DEP_PER_EMP_STDEV = 1
PROJECTS_PER_EMPLOYEE = 2
PROJ_PER_EMP_STDEV = 1
PROJECTS_PER_DEPARTMENT = 5
PROJ_PER_DEPT_STDEV = 3
LOCATIONS_LAMBDA = 0.5  # for exponential distribution
HOURS_PER_WEEK = 40
HOURS_STDEV = 5

# Faker random value generator
generator = Factory.create()

# dict to save used ssns
used_ssns = {}

# Output files
emp_file = open('./employee.txt', 'w')
dept_file = open('./department.txt', 'w')
proj_file = open('./project.txt', 'w')
works_on_file = open('./works_on.txt', 'w')
dependent_file = open('./dependent.txt', 'w')
dept_locations_file = open('./dept_locations.txt', 'w')
class employee(object):
    def __init__(self, fname, minit, lname, ssn, bdate, address, sex, salary,
                 superssn, dno):
        self.fname = fname
        self.minit = minit
        self.lname = lname
        self.ssn = ssn
        self.bdate = bdate
        self.address = address
        self.sex = sex
        self.salary = salary
        self.superssn = superssn
        self.dno = dno

    def __str__(self):
        '''
        Return string appropriate for use with Postgres COPY command
        '''
        if self.superssn == None:
            superssn = '\\N'     
        else:
            superssn = str(self.superssn)
        return self.fname + '\t' + self.minit + '\t' + self.lname + '\t' \
               + self.ssn + '\t' + str(self.bdate) + '\t' + self.address + '\t' \
               + self.sex + '\t' + str(self.salary) + '\t' + \
               superssn + '\t' + str(self.dno)

class dependent(object):
    ''' dependent of an employee
    '''
    
    def __init__(self, essn, fname, gender, bdate, relationship):
        self.essn = essn
        self.fname = fname
        self.gender = gender
        self.bdate = bdate
        self.relationship = relationship

    def __str__(self):
        return self.essn + '\t' + self.fname + '\t' + self.gender + '\t' + \
               str(self.bdate) + '\t' + self.relationship
    

class department(object):
    def __init__(self, dname, dnumber, mgrssn, mgrstartdate):
        self.dname = dname
        self.dnumber = dnumber
        self.mgrssn = mgrssn
        self.mgrstartdate = mgrstartdate

    def __str__(self):
        return self.dname + '\t' + str(self.dnumber) + '\t' + \
               self.mgrssn + '\t' + str(self.mgrstartdate)

class dept_location(object):
    def __init__(self, dnum, dlocation):
        self.dnum = dnum
        self.dlocation = dlocation

    def __str__(self):
        return str(self.dnum) + '\t' + self.dlocation

class project(object):
    def __init__(self, pname, pnumber, plocation, dnum):
        self.pname = pname
        self.pnumber = pnumber
        self.plocation = plocation
        self.dnum = dnum

    def __str__(self):
        return self.pname + '\t' + str(self.pnumber) + '\t' + \
               self.plocation + '\t' + str(self.dnum)

class works_on(object):
    def __init__(self, essn, pno, hours):
        self.essn = essn
        self.pno = pno
        self.hours = hours

    def __str__(self):
        return self.essn + '\t' + str(self.pno) + '\t' + str(self.hours)

def gen_normal_posint(mean, stdev):
    value = round(normalvariate(mean-1, stdev))
    if value < 0:
        value = 0
    return int(value + 1)

def gen_digit_str(count):
    ''' Generate strings of decimal digit characters
    '''
    result = ''
    while count > 0:
        result += choice('0123456789')
        count -= 1
    return result

def gen_initial():
    '''Return random capital letter
    '''
    return choice('ABCDEFGHIJKLMNOPQRSTUVWXYZ')

def gen_gender():
    '''Return random gender M/F
    '''
    return choice('FM')

def generate_dependents(emp):
    ''' Generate dependents of the employee argument
    '''
    num_dependents = int(round(normalvariate(DEPENDENTS_PER_EMPLOYEE, DEP_PER_EMP_STDEV)))
    names_used = []
    for dep_index in range(num_dependents):
        gender = gen_gender()
        # Simplifying by only generating sons and daughters
        # Eventually, would want to check for unique names among children
        if gender == 'F':
            relationship = 'daughter'
            unique = False
            while not unique:
                fname = generator.first_name_female()
                unique = fname not in names_used
        else:
            relationship = 'son'
            unique = False
            while not unique:
                fname = generator.first_name_male()
                unique = fname not in names_used
        names_used.append(fname)
        dep = dependent(emp.ssn, fname, gender, generator.date(), relationship)
        dependent_file.write(str(dep) + '\n')

def gen_salary(min_sal, max_sal):
    '''
    Return salary value in the range of min_sal to max_sal
    '''
    SAL_LAMBDA = 20 # for exponential distribution
    sal = int(expovariate(SAL_LAMBDA) * (max_sal-min_sal)) + min_sal
    if sal > max_sal:
        sal = max_sal
    return sal
    
ssn_t1 = [];
def generate_single_employee(superssn, dno):
    ''' Generate fields for one employee
    '''
    unique = False
    while not unique:
        ssn_str = generator.ssn()

        # Faker generates a string ssn with '-'s, so remove those before putting in the record
        ssn = ssn_str[0:3]+ssn_str[4:6]+ssn_str[7:11] # 123-45-6789  //012,45,6789//123456789
        try:
            used_ssns[int(ssn)]#hash table
        except KeyError:
            unique = True

    used_ssns[int(ssn)] = True
    gender = gen_gender()
    if gender == 'F':
        fname = generator.first_name_female()
    else:
        fname = generator.first_name_male()
        
    new_emp = employee(fname,
                       gen_initial(),
                       generator.last_name(),
                       ssn,
                       generator.date(),
                       generator.address().replace('\n',', '), # replace newline
                       gender,
                       gen_salary(MIN_SALARY, MAX_SALARY),
                       superssn,
                       dno)
    generate_dependents(new_emp)
    emp_file.write(str(new_emp) + '\n')
    return new_emp

ssn_t2 = []
def generate_single_employee_tier(superssn, dno, tier, file):
    ''' Generate fields for one employee
    '''
    unique = False
    while not unique:
        ssn_str = generator.ssn()

        # Faker generates a string ssn with '-'s, so remove those before putting in the record
        ssn = ssn_str[0:3]+ssn_str[4:6]+ssn_str[7:11] # 123-45-6789  //012,45,6789//123456789
        try:
            used_ssns[int(ssn)]#hash table
        except KeyError:
            unique = True

    used_ssns[int(ssn)] = True
    gender = gen_gender()
    if gender == 'F':
        fname = generator.first_name_female()
    else:
        fname = generator.first_name_male()
    if tier == 1:
        ssn_t1.append(ssn)
    elif tier == 2:
        superssn = ssn_t1[randint(0, len(ssn_t1) - 1)]
        ssn_t2.append(ssn)
    new_emp = employee(fname,
                       gen_initial(),
                       generator.last_name(),
                       ssn,
                       generator.date(),
                       generator.address().replace('\n',', '), # replace newline
                       gender,
                       gen_salary(MIN_SALARY, MAX_SALARY),
                       superssn,
                       dno)
    generate_dependents(new_emp)
    file.write(str(new_emp) + '\n')
    return new_emp

def generate_department(dname, dnumber, mgrssn, mgrstartdate):
    '''
    Create department object and save it to the file of department records
    '''
    dept = department(dname, dnumber, mgrssn, mgrstartdate)
    dept_file.write(str(dept) + '\n')
    return dept

def picked_as_supervisor():
    ''' Randomly return True if next employee should (potentially) be a supervisor
    Idea is to queue the employee to generate supervisees, but if we generate enough
    total employees first, we might stop before actually generating supervisees
    '''
    return randint(0, 100) <= PERCENT_SUPERVISORS

def generate_projects(depts):
    '''
    Generate projects as 2-dimensional list so that
    - the first list is the list of projects for the first department,
    - the 2nd list is the list of projects for the 2nd department,
    - and so on
    For now, we are skipping the first (HQ) department
    '''
    next_pnum = 1
    pnum = 1
    projects = [[]] # no projects in HQ department
    for dnum in range(HQ + 1, NUM_DEPTS):
        num_projects = gen_normal_posint(PROJECTS_PER_DEPARTMENT, PROJ_PER_DEPT_STDEV)
        dept_projects = []
        for proj_index in range(num_projects):
            # Using random last names as project names
            # Would be more accurate to check for uniqueness, but since we are using
            # pnum for the key, will skip the check for now
            next_proj = project(generator.last_name(), pnum, generator.city(), dnum)
            proj_file.write(str(next_proj) + '\n')
            dept_projects.append(next_proj)
            pnum += 1
        projects.append(dept_projects)
    return projects

def generate_works_on(emp, dept_projects):
    num_worked_on = gen_normal_posint(PROJECTS_PER_EMPLOYEE, PROJ_PER_EMP_STDEV)
    if num_worked_on > len(dept_projects):
        num_worked_on = len(dept_projects)
    mean_time = HOURS_PER_WEEK / num_worked_on
    shuffle(dept_projects)
    for proj_worked in range(num_worked_on):
        # may end up working a bit more or a bit less than 40 hours right now
        next_works_on = works_on(emp.ssn, dept_projects[proj_worked].pnumber,
                                          gen_normal_posint(mean_time, HOURS_STDEV))
        works_on_file.write(str(next_works_on) + '\n')

def generate_dept_locations(dept):
    num_locations = int(expovariate(LOCATIONS_LAMBDA)) + 1
    locations = []
    
    for loc_index in range(num_locations):
        unique = False
        while not unique:
            city = generator.city()
            locations.append(city)
            unique = city in locations
        next_loc = dept_location(dept.dnumber, city)
        dept_locations_file.write(str(next_loc) + '\n')
        
    
def generate_employees(num_employees):
    '''
    Generate employee records to be at least as many as the desired number
    Do this in a breadth-first approach starting from the CEO, then department
    heads, then the "supervisees" of the department heads, and so on
    '''
    input_file = open('./01.manager_t1.txt', 'w')
    # loop 
    generate_single_employee_tier(superssn=None, dno=HQ, tier=1, file=input_file)
    input_file.close()
    input_file = open('./02.manager_t1.txt', 'w')
    # loop
    generate_single_employee_tier(superssn=None, dno=HQ, tier=2, file=input_file)
    input_file.close()
    return

    # generate CEO first
    ceo = generate_single_employee(superssn=None, dno=HQ)
    hq = generate_department(DEPT_NAMES[0], HQ, ceo.ssn, generator.date())
    generate_dept_locations(hq)
    depts = [hq]

    supervisors = Queue()
    # now generate department managers
    for deptno in range(2, NUM_DEPTS):
        mgr = generate_single_employee(superssn=ceo.ssn, dno=deptno)
        supervisors.put(mgr)
        dept = generate_department(DEPT_NAMES[deptno-1], deptno, mgr.ssn, generator.date())
        depts.append(dept)
        generate_dept_locations(dept)
    dept_file.close()
    dept_locations_file.close()
    employee_ct = NUM_DEPTS

    projects = generate_projects(depts)
    proj_file.close()
	# generate employees
    while employee_ct < num_employees:
        next_supervisor = supervisors.get()
        num_supervisees = randint(MIN_SUPERVISEES, MAX_SUPERVISEES)

        for emp in range(num_supervisees):
            next_emp = generate_single_employee(next_supervisor.ssn, next_supervisor.dno)
            generate_works_on(next_emp, projects[next_emp.dno-1])
            if picked_as_supervisor():
                supervisors.put(next_emp)
        employee_ct += num_supervisees
        # print("employee_ct = %d" % employee_ct)
    emp_file.close()
    dependent_file.close()
    works_on_file.close()
        
# above are class and function definitions. python code starts at main.
# if no argument, will generate 1000000 records.. too many.
# if you put argument, will generate sys.argv[1] (the argument number ) of records
if __name__ == '__main__':
    import sys
    if len(sys.argv) == 1:
        to_generate = 100
    else:
        to_generate = int(sys.argv[1])
    generate_employees(to_generate)
             

