#! /bin/bash

#grep command
set -x
echo "please enter the word to search"
set +x
# read word_to_search
# -c words count
# -n line number
# -i case sensetive turned off
awk '/linux/ {print}' tmp.txt
exit 1



#grep command
echo "please enter the word to search"
read word_to_search
# -c words count
# -n line number
# -i case sensetive turned off
grep -i -n -c $word_to_search tmp.txt
exit 1


for i in {1..5}
do
	echo "This is a linux"
	echo "This is a mac"
	echo "This is a marianna"
done >> tmp.txt
exit 1


#waiting the input from the user
echo "Please press any key to continue"
while [[ true ]]
do
	read -t 3 -n 1
	if [[ $? = 0 ]]; then
		echo "You have pressed some key to exit"
		exit 1
	else 
		echo "...press any key spam msg ..."
	fi
done
exit 1



#select stuff -> like a menu
select car in "honda" "bmw" "other_car"
do
	echo "You have selected: $car"
done
exit 1



#curl
curl "www.google.com" > google.txt
exit 1



#directories
echo "Please enter the directory name to check:"
read directory_name
# -d check if directory with a name exists or not
# -f check if file with a name exists or not
if [[ -d "$directory_name" ]]; then 
	echo "Directory with a name: $directory_name already exists"
else 
	echo "Directory with a name doesn't exists"
fi
exit 1



#directories
echo "Please enter the directory name: "
read directory_name
mkDir $directory_name
echo "Directory was created: $(ls)"
exit 1



#functions 2
function fee_two() {
	inside_variable="linux"
}
inside_variable="mac"
echo "$inside_variable" #will print mac
fee_two
echo "$inside_variable" # will print linux because function was called
exit 1



#functions 1
variable=123
function fee_one() {
	variable2=321
	echo "function body inside_variable: $variable"
	echo "function arguments: $1, $2, $3"
}
fee_one 'aaa' 123 'bbb'
echo "value inside a function: $variable2"
exit 1



#array
cars=('bmw' 'toyota' 'honda')
cars_tmp=$cars
echo "${cars[@]}"   # print all of them
echo "${!cars[@]}"  # print indexes
echo "${#cars[@]}"  # print indexes
echo "${cars[0]}" 	# print lenght
echo "${cars[1]}"
echo "${cars[2]}"
echo "${cars[3]}"

unset cars[2]   # remove second element from an array
cars[2]='mersedes'
echo "${cars[@]}"
exit 1



#math operation
n1=2
n2=3
echo $( expr $n1 + $n2 )
echo $(( 1 + 2 ))
exit 1



#comparison
s1="a"
s2="b"
s3=$s1$s2
echo $s3 #ab
if [ $s1 == $s2 ]; then
	echo "string same"
else 
	echo "not same"
fi
		# compare lenght
if [ "$s1" \> "$s2" ]; then
	echo "string same"
else 
	echo "not same"
fi
exit 1

ls +a >>ok_not_ok.txt 2>&1
exit 1

ls -a 1>>ok.txt 2>>errors_are_goint_to_be_saved_here.txt
exit 1



while read line
do
	echo "Line from file: $line"
done < $1
exit 1



#files
# ./bash_commands.sh bmw audi reno
echo $@ #unlimited numbers of inputs
exit 1

echo $1 $2 $3
exit 1



#for in
for i in {1..10}
do
	if [[ $i == 2 ]]; then
		continue
	fi
	echo "current is: $i"  # with a continue this will not be printed for $i=2
	if [[ $i == 5 ]]; then
		echo "going to breake"
		break
	fi
done
exit 1



#for in
for i in 1 2 3 4 5
do
	echo "current is: $i"
done
exit 1



#until loop. Will run until condition is false
number=12
until [[ $number -le 0 ]]
do
	echo "The number is: $number"
	number=$(( number-1 ))
done
exit 1



#while loop. Will run until condition is true
number=12
while [[ $number -gt 0 ]]
do
	echo "The number is: $number"
	number=$(( number-1 ))
done
exit 1



#case statement
car="audias"
case $car in
	"bmw")
		echo "The car is bmw" ;;
	"audi")
		echo "The car is audi" ;;
	*)
		echo "Some unknown car" ;;
esac	
exit 1



# if condition.
# for String should be used with a `==`, integers is ok with `-eq, -gt, -lt`
name="1hello"
another_name="another_name"
age=18
if [[ $name == "hello" && $age -eq 10 || $another_name == "foo" ]]; then
	echo "true condition"
elif [[ $name == "1hello" && $age -lt 10 || $another_name == "foo2" ]]; then
	echo "another else condition"
elif [[ $another_name == "another_name" ]]; then
	echo "another_name condition"
else
	echo "false condition"
fi
exit 1



#read from console and write into the file
cat > file.txt
exit 1
: 'multiline comment. read from console and append into the file'
cat >> file.txt
exit 1

