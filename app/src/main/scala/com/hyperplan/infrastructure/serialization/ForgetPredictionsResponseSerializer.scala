/*
 *
 * This file is part of the Hyperplan project.
 * Copyright (C) 2019  Hyperplan
 * Authors: Antoine Sauray
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gn/u.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the Hyperplan software without
 * disclosing the source code of your own applications.
 *
 *
 */

package com.hyperplan.infrastructure.serialization

import io.circe.syntax._
import io.circe._

import com.hyperplan.application.controllers.responses._

object ForgetPredictionsResponseSerializer {

  implicit val encoder: Encoder[ForgetPredictionsResponse] =
    (response: ForgetPredictionsResponse) =>
      Json.obj(
        ("entityName", Json.fromString(response.entityName)),
        ("entityId", Json.fromString(response.entityId)),
        ("count", Json.fromInt(response.count))
      )

  def encodeJson(response: ForgetPredictionsResponse) =
    response.asJson

}
