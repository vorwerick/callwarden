package cz.dzubera.callwarden.data

import cz.dzubera.callwarden.model.Call

class MockCallRecords {
    companion object {
        fun getMockCallRecords(): List<Call> {
           return listOf(
               Call(0,"0","0","0", "Firemní hovory", 12, Call.Direction.INCOMING, "243 330 330",1692613060, 0, 0),
               Call(0,"0","0","0", "Soukromé hovory", 102, Call.Direction.INCOMING, "700 200 120",1692613000, 0, 0),
               Call(0,"0","0","0", "Soukromé hovory", 440, Call.Direction.OUTGOING, "340 209 900",1692614200, 0, 0),
               Call(0,"0","0","0", "Soukromé hovory", 0, Call.Direction.INCOMING, "340 209 900",1692614100, 0, 0),
               Call(0,"0","0","0", "Firemní hovory", 0, Call.Direction.OUTGOING, "400 600 100",169261359, 0, 0)



           )
        }
    }
}