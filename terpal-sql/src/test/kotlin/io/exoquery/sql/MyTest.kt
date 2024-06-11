package io.exoquery.sql

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

data class Result(val title: String, val batches: Int, val batchSize: Int, val wait: Int, val totalTimeMillis: Long) {
  fun tsv() =
    "${title}\t${batches}\t${batchSize}\t${totalTimeMillis}"
}


data class RowData(val batches: Int, val batchSize: Int, val wait: Int)

class RowMaker(val rd: RowData) {
  val batches = rd.batches
  val batchSize = rd.batchSize
  val wait = rd.wait

  var currBatch = 0
  var curr = 0
  fun max() = batches * batchSize

  fun hasNext(): Boolean =
    curr < max()

  fun next(): List<String> {
    val output = (0 until batchSize).map { numInBatch ->
      val output = "${currBatch}-${numInBatch}-${curr}"
      curr++
      output
    }.toList()
    currBatch++
    if (wait > 0) Thread.sleep(wait.toLong())
    return output
  }

  override fun toString(): String =
    "Batches:${batches}, BatchSize:${batchSize}, Wait:${wait}"
}

suspend fun collect1(rowData: RowData): Flow<String> {
  val eb = RowMaker(rowData)
  val list = mutableListOf<String>()
  while (eb.hasNext()) {
    list.addAll(eb.next())
  }
  return flowOf(*list.toTypedArray())
}

suspend fun collect2(rowData: RowData): Flow<String> {
  val eb = RowMaker(rowData)
  return flow {
    while (eb.hasNext()) {
      val next = eb.next()
      for (elem in next) {
        emit(elem)
      }
    }
  }
}


suspend fun collect3(rowData: RowData): Flow<String> {
  val eb = RowMaker(rowData)
  return flow {
    val list = mutableListOf<String>()
    while (eb.hasNext()) {
      val next = eb.next()
      list.addAll(next)
      emitAll(flowOf(*list.toTypedArray()))
      list.clear()
    }
  }
}


suspend fun collect4(rowData: RowData): Flow<String> {
  val eb = RowMaker(rowData)
  return flow {
    val list = Array(eb.batchSize) { _ -> "" }
    while (eb.hasNext()) {
      val next = eb.next()
      for (i in (0 until eb.batchSize)) {
        list[i] = next[i]
      }
      emitAll(flowOf(*list))
    }
  }
}

suspend fun collectAndMeasure(title: String, eb: RowData, flow: Flow<String>, incrementPrint: Int = -1): Result {
  //println("${title} - Starting measurement")
  val start = System.currentTimeMillis()
  var increment = 0
  val count =
    flow.map { value ->
      if (incrementPrint > 0) {
        if (increment % incrementPrint == 0) {
          println("Increment: ${increment} - ${value}")
        }
      }
      increment++
    }.count()
  val totalTimeMillis = System.currentTimeMillis() - start
  println("${title} - Total Time: ${(totalTimeMillis).toDouble()/1000} - ${count} elements")
  return Result(title, eb.batches, eb.batchSize, eb.wait, totalTimeMillis)
}

fun experiment(eb: RowData, warmups: Int): Pair<Result, Result> {
  val individualResult =
  runBlocking {
    for (i in (0 until warmups)) {
      collectAndMeasure("individual-emit", eb, collect2(eb))
    }
    collectAndMeasure("individual-emit", eb, collect2(eb))
  }
  val listResult =
  runBlocking {
    for (i in (0 until warmups)) {
      collectAndMeasure("list-emit", eb, collect3(eb))
    }
    collectAndMeasure("list-emit", eb, collect3(eb))
  }
  return individualResult to listResult
}

fun main() {
  val individualEmits = mutableListOf<Result>()
  val listEmits = mutableListOf<Result>()

  fun addToResults(results: Pair<Result, Result>) {
    val (ind, list) = results
    individualEmits.add(ind)
    listEmits.add(list)
  }
  fun printResults() {
    individualEmits.forEach { println(it.tsv()) }
    listEmits.forEach { println(it.tsv()) }
  }

  //addToResults(experiment(RowData(1000000, 1, 1), 10))
  //addToResults(experiment(RowData(100000, 10, 10), 10))
  addToResults(experiment(RowData(10000, 100, 1), 1))
  addToResults(experiment(RowData(1000, 1000, 10), 1))
  addToResults(experiment(RowData(100, 10000, 100), 1))
  addToResults(experiment(RowData(10, 100000, 1000), 1))
  //addToResults(experiment(RowData(1, 1000000, 0), 10))

  printResults()

}
