// https://www.youtube.com/watch?v=bum_19loj9A&list=PLBZBJbE_rGRV8D7XZ08LK6z-4zPoWzu5H
// https://www.youtube.com/watch?v=ZRdOb4yR0kk
/*
  Example 1
  Функция рекурсивная - имеент сложность O(N)
  - потому что єта функция будет визиватся столько раз каково значение входного параметра `a: Int`
    check(1) // 1
    check(2) // 2 + 1
    check(3) // 3 + 2 + 1
    check(4) // 4 + 3 + 2 + 1
*/
def check(a: Int): Int =
  if (a == 1) 1
  else a + check(a - 1)

/* Example 2 */
def check2(a: Int): Unit = {

  /* сложность О(1) -> не зависит от количества входниз аргументов - нету циклов */
  def pairSum(a: Int, b: Int): Int = a + b

  /* сложность О(N) -> в зависимости от числа фунция будет виполнятся `a` количество раз. Потомучто єсть цикл */
  def pairSumSequence(a: Int): Int = (0 to a).fold(0)((acc, el) => acc + pairSum(el, el + 1))
}


/* Example 3
Сложность O(N) потому что єсть цикл которий напрямую зависит от количества елементов в месиве и ми должни пройтись
по всех елементах масива

int min = Integer.MIN_VALUE
int max = Integer.MAX_VALUE
for (int x: array) {
  if (x < min) min = x
  if (x > max) max = x
}


Сложность O(N) потому что єсть цикл которий напрямую зависит от количества елементов в месиве и ми должни пройтись
по всех елементах масива дважди. В итоге получається:   O(N) + O(N) => O(N)

int min = Integer.MIN_VALUE
int max = Integer.MAX_VALUE
for (int x: array) {
  if (x < min) min = x
}
for (int x: array) {
  if (x > max) max = x
{



Example 3

Для алгортма где на каждой итерации берется половина елементов
сложность будет включать О(logN)
Пример, поиск по отсортированому масиву.



* */





