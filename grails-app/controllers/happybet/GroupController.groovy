package happybet

import grails.plugin.springsecurity.annotation.Secured

@Secured(['ROLE_GROUP'])
class GroupController {

    def betService
    def userService

    def index() {
        render view: 'index', model: [groups: BetGroup.findAllByOwner(request.getRemoteUser())]
    }

    def matches () {
        def match = new BetMatch(params)
        if (params.matchId) {
            match = BetMatch.findById(params.matchId)
        }

        render view: 'matches', model: [groupId: params.groupId, matchId: params.matchId,
                                       betMatchInstance: match, teams: Team.list()]
    }

    def save() {
        if (params.matchId)
            betService.updateMatch(params.matchId, params.home, params.guess, params.date, Integer.valueOf(params.hScore),
                    Integer.valueOf(params.gScore), Float.valueOf(params.hRate), Float.valueOf(params.gRate),
                    Double.valueOf(params.amount))
        else
            betService.createMatch(params.group, params.home, params.guess, params.date, Integer.valueOf(params.hScore),
                    Integer.valueOf(params.gScore), Float.valueOf(params.hRate), Float.valueOf(params.gRate),
                    Double.valueOf(params.amount))
        redirect(controller: 'group')
    }

    def remove() {
        betService.deleteMatch(params.groupId, params.matchId)
        index()
    }

    def member() {
        render view: 'member', model: [groupId: params.groupId]
    }

    def addUser() {
        def errMsg = betService.addUserToGroup(params.group, params.email)
        if (errMsg) {
            flash.error = errMsg
        } else {
            flash.message = 'Added ' + params.email + ' to group successfully.'
        }
        member()
    }

    def delUser() {
        betService.removeUserFromGroup(params.groupId, params.userId)
        index()
    }

    def forget() {
        render(view: 'forget')
    }

    def reset() {
        def groups = BetGroup.findAllByOwner(request.getRemoteUser())
        def check = false
        groups.each {group ->
            group.users.each {user ->
                if (user.username.equals(params.email))
                    check = true
            }
        }
        if (check) {
            def errMsg = userService.resetPassword(params.email)
            if (errMsg) {
                flash.error = errMsg
            } else {
                flash.message = 'Please check your email for your credential.'
            }
        } else {
            flash.error = 'User not found in your group'
        }

        render(view: 'forget')
    }

    def report() {
        def group = BetGroup.findByOwner(request.getRemoteUser())
        def matches = BetMatch.findAllByGroup(group)
        def summary
        def bets = []
        if (params.matches) {
            def ids = []
            params.matches.each { m ->
                ids.add(Long.valueOf(m))
            }
            summary = Bet.createCriteria().list {
                createAlias('match', 'm')
                inList('m.id', ids)
                projections {
                    groupProperty('owner')
                    sum('amount')
                }
                order('amount', 'desc')
            }
            bets = Bet.createCriteria().list {
                createAlias('match', 'm')
                inList('m.id', ids)
            }
        }
        render(view: 'report', model: [matches: matches, bets: bets, sum: summary])
    }
}
