/*
 * Copyright (c) 2014-2017 by The Monix Project Developers.
 * See the project homepage at: https://monix.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package monix.reactive.internal.operators

import monix.execution.Ack
import monix.execution.Ack.{Continue, Stop}
import monix.reactive.Observable.Operator
import monix.reactive.observers.Subscriber
import scala.concurrent.Future

private[reactive] final
class WhileBusyDropEventsOperator[A] extends Operator[A,A] {

  def apply(out: Subscriber[A]): Subscriber.Sync[A] =
    new Subscriber.Sync[A] {
      implicit val scheduler = out.scheduler

      private[this] var ack = Continue : Future[Ack]
      private[this] var isDone = false

      def onNext(elem: A) =
        if (isDone) Stop else
          ack.syncTryFlatten match {
            case Continue =>
              ack = out.onNext(elem)
              Continue
            case Stop => Stop
            case async => Continue
          }

      def onError(ex: Throwable) =
        if (!isDone) {
          isDone = true
          out.onError(ex)
        }

      def onComplete() =
        if (!isDone) {
          isDone = true
          out.onComplete()
        }
    }
}