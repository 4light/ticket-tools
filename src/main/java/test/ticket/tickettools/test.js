;(window.webpackJsonp = window.webpackJsonp || []).push([
    [13, 5],
    {
        657: function (e, t, n) {
            'use strict'
            n.r(t)
            var o = {
                    props: { show: { type: Boolean, default: !1 } },
                    data: function () {
                        return { show2: !1, bulletin: '' }
                    },
                    watch: {
                        '$i18n.locale': function () {
                            this.getData()
                        },
                    },
                    mounted: function () {
                        this.getData()
                    },
                    methods: {
                        getData: function () {
                            var e = this
                            this.$axios
                                .$post('/ajax?ugi=start&action=bullet', {
                                    bullettype: 'HOMEPAGE',
                                })
                                .then(function (t) {
                                    'A00006' === t.code &&
                                    ((e.bulletin = t.content), (e.show2 = e.show))
                                })
                        },
                    },
                },
                r = n(43),
                component = Object(r.a)(
                    o,
                    function () {
                        var e = this,
                            t = e._self._c
                        return t(
                            'div',
                            [
                                e.bulletin
                                    ? t('div', {
                                        staticClass: 'yynoticetipicon',
                                        class: { en: 'en' === e.$i18n.locale },
                                        on: {
                                            click: function (t) {
                                                e.show2 = !e.show2
                                            },
                                        },
                                    })
                                    : e._e(),
                                e._v(' '),
                                t(
                                    'el-dialog',
                                    {
                                        attrs: {
                                            'custom-class': 'dialog-modal',
                                            'lock-scroll': !1,
                                            visible: e.show2,
                                        },
                                        on: {
                                            'update:visible': function (t) {
                                                e.show2 = t
                                            },
                                        },
                                    },
                                    [
                                        t('div', { attrs: { slot: 'title' }, slot: 'title' }, [
                                            t('h5', { staticClass: 'header' }, [
                                                e._v(e._s(e.$t('JNT.session.noticeInfo'))),
                                            ]),
                                        ]),
                                        e._v(' '),
                                        t('div', {
                                            staticStyle: {
                                                'min-height': '250px',
                                                'max-height': '400px',
                                                overflow: 'auto',
                                            },
                                            domProps: { innerHTML: e._s(e.bulletin) },
                                        }),
                                        e._v(' '),
                                        t(
                                            'div',
                                            { staticClass: 'btn-group' },
                                            [
                                                t(
                                                    'el-button',
                                                    {
                                                        staticStyle: { width: '200px' },
                                                        attrs: { type: 'primary' },
                                                        on: {
                                                            click: function (t) {
                                                                e.show2 = !1
                                                            },
                                                        },
                                                    },
                                                    [e._v(e._s(e.$t('JNT.session.iKnow')))]
                                                ),
                                            ],
                                            1
                                        ),
                                    ]
                                ),
                            ],
                            1
                        )
                    },
                    [],
                    !1,
                    null,
                    null,
                    null
                )
            t.default = component.exports
        },
        675: function (e, t) {
            e.exports =
                'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAABq0lEQVRoQ+1YbZbDIAjEm7U34gbbniDeaNuTbZ95Zp9RIvgRm7T4szXowIADBk6+zMnvDysAiHjpCcha+3D29rLrbMcA/joCeFhrrx7ALwB0c4619v/eCiATMY3AlnNKKTQnomDFHOciILXrjl7ZLgIQbt4C4auMS9RwcQCuS5XKOQcRkwLwnQAQ8YcohXfnxZYI+G+d7dXKleCqCHgAt+icmQYdABTRTwEEUdAI1NCvlkKklumUA0klDYTgl5dRAJgVpWAVVZICu0n5LqKQ4OJbW7iXuNq0AhC6TiNQI6fjRBQ6m94W6ZkmW+HHi13322dNJbq5aKChuKkfSaFZinNYfUNDSu2EQog4cqwypiPjPJT5P1tGjTGXaZqenP3mlpI7QAEwHuoRgR5ijpLiSR8MAEliU3PVMPnZ0WKPsQrlZMqzTqFKKlNoTwzAN/XxXZ6ShuZIAKqmEgogk+ilFNo1AtTb0FyFliRuGWxJKTQCwK5JvCuA3HvDzXYOEYHTAhiphTYet+QKh5hKSPlOOVABCGnFjVUS3XMqClHCrRmA0LOH2qZjlXeH4wVwZJNPKrseCQAAAABJRU5ErkJggg=='
        },
        947: function (e, t, n) {
            var map = {
                './af': 718,
                './af.js': 718,
                './ar': 719,
                './ar-dz': 720,
                './ar-dz.js': 720,
                './ar-kw': 721,
                './ar-kw.js': 721,
                './ar-ly': 722,
                './ar-ly.js': 722,
                './ar-ma': 723,
                './ar-ma.js': 723,
                './ar-sa': 724,
                './ar-sa.js': 724,
                './ar-tn': 725,
                './ar-tn.js': 725,
                './ar.js': 719,
                './az': 726,
                './az.js': 726,
                './be': 727,
                './be.js': 727,
                './bg': 728,
                './bg.js': 728,
                './bm': 729,
                './bm.js': 729,
                './bn': 730,
                './bn-bd': 731,
                './bn-bd.js': 731,
                './bn.js': 730,
                './bo': 732,
                './bo.js': 732,
                './br': 733,
                './br.js': 733,
                './bs': 734,
                './bs.js': 734,
                './ca': 735,
                './ca.js': 735,
                './cs': 736,
                './cs.js': 736,
                './cv': 737,
                './cv.js': 737,
                './cy': 738,
                './cy.js': 738,
                './da': 739,
                './da.js': 739,
                './de': 740,
                './de-at': 741,
                './de-at.js': 741,
                './de-ch': 742,
                './de-ch.js': 742,
                './de.js': 740,
                './dv': 743,
                './dv.js': 743,
                './el': 744,
                './el.js': 744,
                './en-au': 745,
                './en-au.js': 745,
                './en-ca': 746,
                './en-ca.js': 746,
                './en-gb': 747,
                './en-gb.js': 747,
                './en-ie': 748,
                './en-ie.js': 748,
                './en-il': 749,
                './en-il.js': 749,
                './en-in': 750,
                './en-in.js': 750,
                './en-nz': 751,
                './en-nz.js': 751,
                './en-sg': 752,
                './en-sg.js': 752,
                './eo': 753,
                './eo.js': 753,
                './es': 754,
                './es-do': 755,
                './es-do.js': 755,
                './es-mx': 756,
                './es-mx.js': 756,
                './es-us': 757,
                './es-us.js': 757,
                './es.js': 754,
                './et': 758,
                './et.js': 758,
                './eu': 759,
                './eu.js': 759,
                './fa': 760,
                './fa.js': 760,
                './fi': 761,
                './fi.js': 761,
                './fil': 762,
                './fil.js': 762,
                './fo': 763,
                './fo.js': 763,
                './fr': 764,
                './fr-ca': 765,
                './fr-ca.js': 765,
                './fr-ch': 766,
                './fr-ch.js': 766,
                './fr.js': 764,
                './fy': 767,
                './fy.js': 767,
                './ga': 768,
                './ga.js': 768,
                './gd': 769,
                './gd.js': 769,
                './gl': 770,
                './gl.js': 770,
                './gom-deva': 771,
                './gom-deva.js': 771,
                './gom-latn': 772,
                './gom-latn.js': 772,
                './gu': 773,
                './gu.js': 773,
                './he': 774,
                './he.js': 774,
                './hi': 775,
                './hi.js': 775,
                './hr': 776,
                './hr.js': 776,
                './hu': 777,
                './hu.js': 777,
                './hy-am': 778,
                './hy-am.js': 778,
                './id': 779,
                './id.js': 779,
                './is': 780,
                './is.js': 780,
                './it': 781,
                './it-ch': 782,
                './it-ch.js': 782,
                './it.js': 781,
                './ja': 783,
                './ja.js': 783,
                './jv': 784,
                './jv.js': 784,
                './ka': 785,
                './ka.js': 785,
                './kk': 786,
                './kk.js': 786,
                './km': 787,
                './km.js': 787,
                './kn': 788,
                './kn.js': 788,
                './ko': 789,
                './ko.js': 789,
                './ku': 790,
                './ku.js': 790,
                './ky': 791,
                './ky.js': 791,
                './lb': 792,
                './lb.js': 792,
                './lo': 793,
                './lo.js': 793,
                './lt': 794,
                './lt.js': 794,
                './lv': 795,
                './lv.js': 795,
                './me': 796,
                './me.js': 796,
                './mi': 797,
                './mi.js': 797,
                './mk': 798,
                './mk.js': 798,
                './ml': 799,
                './ml.js': 799,
                './mn': 800,
                './mn.js': 800,
                './mr': 801,
                './mr.js': 801,
                './ms': 802,
                './ms-my': 803,
                './ms-my.js': 803,
                './ms.js': 802,
                './mt': 804,
                './mt.js': 804,
                './my': 805,
                './my.js': 805,
                './nb': 806,
                './nb.js': 806,
                './ne': 807,
                './ne.js': 807,
                './nl': 808,
                './nl-be': 809,
                './nl-be.js': 809,
                './nl.js': 808,
                './nn': 810,
                './nn.js': 810,
                './oc-lnc': 811,
                './oc-lnc.js': 811,
                './pa-in': 812,
                './pa-in.js': 812,
                './pl': 813,
                './pl.js': 813,
                './pt': 814,
                './pt-br': 815,
                './pt-br.js': 815,
                './pt.js': 814,
                './ro': 816,
                './ro.js': 816,
                './ru': 817,
                './ru.js': 817,
                './sd': 818,
                './sd.js': 818,
                './se': 819,
                './se.js': 819,
                './si': 820,
                './si.js': 820,
                './sk': 821,
                './sk.js': 821,
                './sl': 822,
                './sl.js': 822,
                './sq': 823,
                './sq.js': 823,
                './sr': 824,
                './sr-cyrl': 825,
                './sr-cyrl.js': 825,
                './sr.js': 824,
                './ss': 826,
                './ss.js': 826,
                './sv': 827,
                './sv.js': 827,
                './sw': 828,
                './sw.js': 828,
                './ta': 829,
                './ta.js': 829,
                './te': 830,
                './te.js': 830,
                './tet': 831,
                './tet.js': 831,
                './tg': 832,
                './tg.js': 832,
                './th': 833,
                './th.js': 833,
                './tk': 834,
                './tk.js': 834,
                './tl-ph': 835,
                './tl-ph.js': 835,
                './tlh': 836,
                './tlh.js': 836,
                './tr': 837,
                './tr.js': 837,
                './tzl': 838,
                './tzl.js': 838,
                './tzm': 839,
                './tzm-latn': 840,
                './tzm-latn.js': 840,
                './tzm.js': 839,
                './ug-cn': 841,
                './ug-cn.js': 841,
                './uk': 842,
                './uk.js': 842,
                './ur': 843,
                './ur.js': 843,
                './uz': 844,
                './uz-latn': 845,
                './uz-latn.js': 845,
                './uz.js': 844,
                './vi': 846,
                './vi.js': 846,
                './x-pseudo': 847,
                './x-pseudo.js': 847,
                './yo': 848,
                './yo.js': 848,
                './zh-cn': 849,
                './zh-cn.js': 849,
                './zh-hk': 850,
                './zh-hk.js': 850,
                './zh-mo': 851,
                './zh-mo.js': 851,
                './zh-tw': 852,
                './zh-tw.js': 852,
            }
            function o(e) {
                var t = r(e)
                return n(t)
            }
            function r(e) {
                if (!n.o(map, e)) {
                    var t = new Error("Cannot find module '" + e + "'")
                    throw ((t.code = 'MODULE_NOT_FOUND'), t)
                }
                return map[e]
            }
            ;(o.keys = function () {
                return Object.keys(map)
            }),
                (o.resolve = r),
                (e.exports = o),
                (o.id = 947)
        },
        983: function (e, t, n) {
            'use strict'
            n.r(t)
            n(28), n(12), n(53), n(25), n(51), n(52)
            var o = n(17),
                r = n(6),
                c = (n(68), n(20), n(60), n(369), n(34), n(33), n(180), n(77), n(646)),
                l = n.n(c),
                d = n(677),
                m = n.n(d),
                f = n(854)
            function h(object, e) {
                var t = Object.keys(object)
                if (Object.getOwnPropertySymbols) {
                    var n = Object.getOwnPropertySymbols(object)
                    e &&
                    (n = n.filter(function (e) {
                        return Object.getOwnPropertyDescriptor(object, e).enumerable
                    })),
                        t.push.apply(t, n)
                }
                return t
            }
            function v(e) {
                for (var i = 1; i < arguments.length; i++) {
                    var source = null != arguments[i] ? arguments[i] : {}
                    i % 2
                        ? h(Object(source), !0).forEach(function (t) {
                            Object(o.a)(e, t, source[t])
                        })
                        : Object.getOwnPropertyDescriptors
                        ? Object.defineProperties(
                            e,
                            Object.getOwnPropertyDescriptors(source)
                        )
                        : h(Object(source)).forEach(function (t) {
                            Object.defineProperty(
                                e,
                                t,
                                Object.getOwnPropertyDescriptor(source, t)
                            )
                        })
                }
                return e
            }
            var _ = {
                    watchQuery: ['orderid'],
                    components: { vueQr: m.a, Verify: f.default },
                    asyncData: function (e) {
                        var title,
                            t,
                            n = e.params,
                            o = e.i18n,
                            r = e.error,
                            c = e.query
                        if ('' === n.pathMatch || 'user' === n.pathMatch)
                            (n.pathMatch = 'user'),
                                (title = o.t('JNT.navbar.user')),
                                (t = 'PERSONAL')
                        else if ('tg' === n.pathMatch)
                            (title = o.t('JNT.navbar.tg')), (t = 'GROUP')
                        else {
                            if ('ta' !== n.pathMatch)
                                return void r({ statusCode: 404, message: o.t('JNT.error') })
                                    ;(title = o.t('JNT.navbar.ta')), (t = 'GROUP')
                        }
                        return {
                            orderid: c.orderid,
                            eventssessionid: c.eventssessionid,
                            type: n.pathMatch,
                            fromtype: t,
                            title: title,
                            dateList: [],
                            dateSelected: {},
                            booking_including_self: 0,
                        }
                    },
                    data: function () {
                        return {
                            captchaType: '',
                            agreeTime: 0,
                            agreeTimer: null,
                            agree: !1,
                            baseURL: 'https://jnt.mfu.com.cn',
                            show_notice: !1,
                            selectedTime: null,
                            loading: !1,
                            verifyimg: null,
                            showverify: !1,
                            orderData: null,
                            verifysgow: '',
                        }
                    },
                    computed: {
                        accountType: function () {
                            return this.$store.state.accountType
                        },
                        userInfo: function () {
                            return this.$store.state.userInfo
                        },
                    },
                    created: function () {
                        this.accountType === this.$route.params.pathMatch ||
                        ('user' === this.accountType &&
                            '' === this.$route.params.pathMatch) ||
                        this.$router.replace('/'.concat(this.accountType))
                    },
                    mounted: function () {
                        var e = this
                        this.getSessions(),
                        this.orderid &&
                        this.$axios
                            .$post(
                                '/ajax?ugi='
                                    .concat(
                                        this.$route.params.pathMatch,
                                        '/orderquery&action=orderdetail&orderid='
                                    )
                                    .concat(this.orderid)
                            )
                            .then(function (t) {
                                'A00006' === t.code && (e.orderData = t)
                            })
                    },
                    methods: {
                        getSessions: function () {
                            var e = this
                            return Object(r.a)(
                                regeneratorRuntime.mark(function t() {
                                    var n, o, r, c, d, i, m, f, h, _, T
                                    return regeneratorRuntime.wrap(function (t) {
                                        for (;;)
                                            switch ((t.prev = t.next)) {
                                                case 0:
                                                    return (
                                                        (t.next = 2),
                                                            e.$axios.$post(
                                                                '/ajax?ugi=bookingquery&action=getSessions',
                                                                {
                                                                    fromtype: e.fromtype,
                                                                    siteid: '7e97d18d179c4791bab189f8de87ee9d',
                                                                }
                                                            )
                                                    )
                                                case 2:
                                                    if (((n = t.sent), (o = []), 'A00006' === n.code)) {
                                                        if (
                                                            (r = Object.keys(n)
                                                                .filter(function (e) {
                                                                    return (
                                                                        'code' !== e &&
                                                                        'errmsg' !== e &&
                                                                        'booking_including_self' !== e &&
                                                                        'currdate_booking' !== e
                                                                    )
                                                                })
                                                                .sort(function (a, b) {
                                                                    return a > b ? 1 : -1
                                                                })).length
                                                        ) {
                                                            for (
                                                                c = l()(r[0]),
                                                                    d = l()(r[r.length - 1]),
                                                                    r = [],
                                                                    i = 0;
                                                                i < d.diff(c, 'd') + 1;
                                                                i++
                                                            )
                                                                r.push(
                                                                    c.clone().add(i, 'd').format('YYYY-MM-DD')
                                                                )
                                                            for (m = c.isoWeekday(), f = 0; f < m - 1; f++)
                                                                r.unshift(
                                                                    c.subtract(1, 'd').format('YYYY-MM-DD')
                                                                )
                                                            for (h = d.isoWeekday(), _ = 0; _ < 7 - h; _++)
                                                                r.push(d.add(1, 'd').format('YYYY-MM-DD'))
                                                        } else
                                                            for (T = 0; T < 7; T++)
                                                                r.push(
                                                                    l()()
                                                                        .startOf('isoWeek')
                                                                        .add(T, 'd')
                                                                        .format('YYYY-MM-DD')
                                                                )
                                                        r.forEach(function (e) {
                                                            var t = { date: e, isToday: !1 }
                                                            ;(t.isToday = l()().format('YYYY-MM-DD') === e),
                                                                (t = n[e]
                                                                    ? v(v({}, t), {}, { disabled: !1 }, n[e])
                                                                    : v(v({}, t), {}, { disabled: !0 })),
                                                                o.push(t)
                                                        }),
                                                            o.forEach(function (e) {
                                                                ;(e.hasValidSession = !1),
                                                                e.sessions &&
                                                                (e.sessions.forEach(function (time) {
                                                                    time.isBefore = l()(
                                                                        ''
                                                                            .concat(e.date, ' ')
                                                                            .concat(time.endtime),
                                                                        ['YYYY-MM-DD HH:mm']
                                                                    ).isBefore(l()())
                                                                }),
                                                                    (e.hasValidSession = e.sessions.some(
                                                                        function (e) {
                                                                            return !e.isBefore
                                                                        }
                                                                    )))
                                                            }),
                                                            (e.dateList = o),
                                                            (e.dateSelected =
                                                                o.find(function (e) {
                                                                    return (
                                                                        0 !== e.remaining_total && e.hasValidSession
                                                                    )
                                                                }) || {}),
                                                            (e.booking_including_self =
                                                                ('user' === e.type &&
                                                                    n.booking_including_self) ||
                                                                0)
                                                    } else
                                                        'A00013' === n.code
                                                            ? ((e.verifysgow = 'rq'),
                                                                (e.captchaType = n.captcha_type),
                                                                setTimeout(function () {
                                                                    e.useVerify()
                                                                }, 500),
                                                                (e.verifyimg = n.verifyimg))
                                                            : (e.showverify = !1)
                                                case 5:
                                                case 'end':
                                                    return t.stop()
                                            }
                                    }, t)
                                })
                            )()
                        },
                        success: function () {
                            console.log('1212'),
                                (this.showverify = !1),
                                'rq' === this.verifysgow
                                    ? this.getSessions()
                                    : 'gq' === this.verifysgow && this.rebook()
                        },
                        useVerify: function () {
                            this.$refs.verify.show()
                        },
                        finish: function (data) {
                            var e = this
                            return this.$axios
                                .$post(
                                    '/ajax?ugi=answer&action=submitVerifyData',
                                    v({ verifyreqid: this.verifyimg.verifyreqid }, data),
                                    {
                                        params: { bundleid: 'mpwork.imgverify', moduleid: '' },
                                        headers: {
                                            'Content-Type': 'application/json;charset=utf-8',
                                            post: {
                                                'Content-Type': 'application/json;charset=utf-8',
                                            },
                                        },
                                    }
                                )
                                .then(function (t) {
                                    return 'A00013' === t.code
                                        ? ((e.captchaType = t.captcha_type),
                                            setTimeout(function () {
                                                e.useVerify()
                                            }, 500),
                                            (e.verifyimg = t.verifyimg),
                                            !1)
                                        : 'A00005' === t.code
                                            ? (e.getSessions(), !1)
                                            : 'A00006' === t.code
                                                ? (e.getSessions(), (e.showverify = !1), !0)
                                                : void 0
                                })
                        },
                        selectTime: function (e) {
                            e.isBefore || this.eventssessionid === e.eventssessionid
                                ? this.$message.warning(this.$t('JNT.session.message1'))
                                : 0 === e.remaining || 0 === e.remaining_check
                                ? this.$message.warning(this.$t('JNT.session.message2'))
                                : e.status
                                    ? 2 === e.status
                                        ? this.$message.warning(this.$t('JNT.session.message2'))
                                        : (this.selectedTime = e)
                                    : this.$message.warning(this.$t('JNT.session.message3'))
                        },
                        rebook: function () {
                            var e = this
                            if (!this.selectedTime)
                                return this.$message.warning(this.$t('JNT.session.message4'))
                            this.$confirm(
                                this.$t('JNT.session.message5', {
                                    date: this.dateSelected.date,
                                    begintime: this.selectedTime.begintime,
                                    endtime: this.selectedTime.endtime,
                                }),
                                this.$t('JNT.session.alertTitle'),
                                {
                                    confirmButtonText: this.$t('JNT.session.confirmButtonText'),
                                    cancelButtonText: this.$t('JNT.session.cancelButtonText'),
                                    type: 'warning',
                                }
                            ).then(function () {
                                e.$axios
                                    .$post('/ajax?ugi=bookingorder&action=rebook', {
                                        usertype: e.accountType,
                                        orderid: e.orderid,
                                        to_eventssessionid: e.selectedTime.eventssessionid,
                                    })
                                    .then(function (t) {
                                        'A00006' === t.code
                                            ? (e.$message.success(e.$t('JNT.session.message6')),
                                                e.$router.push('/account/record'))
                                            : 'A00013' === t.code &&
                                            ((e.verifysgow = 'gq'),
                                                (e.captchaType = t.captcha_type),
                                                setTimeout(function () {
                                                    e.useVerify()
                                                }, 500),
                                                (e.verifyimg = t.verifyimg))
                                    })
                            })
                        },
                        handleDateSelected: function (e) {
                            !e.disabled && e.hasValidSession
                                ? this.dateSelected !== e &&
                                ((this.selectedTime = null), (this.dateSelected = e))
                                : this.$message.warning(this.$t('JNT.session.message7'))
                        },
                        handleWriteOrder: function () {
                            var e = this
                            return Object(r.a)(
                                regeneratorRuntime.mark(function t() {
                                    var n
                                    return regeneratorRuntime.wrap(function (t) {
                                        for (;;)
                                            switch ((t.prev = t.next)) {
                                                case 0:
                                                    return (
                                                        (n = {
                                                            name: 'type-editorder-eventssessionid',
                                                            params: {
                                                                type: e.accountType,
                                                                eventssessionid: e.selectedTime.eventssessionid,
                                                            },
                                                            query: {
                                                                date: e.dateSelected.date,
                                                                begintime: e.selectedTime.begintime,
                                                                endtime: e.selectedTime.endtime,
                                                                booking_including_self:
                                                                e.booking_including_self,
                                                                maxnums: e.selectedTime.maxnums,
                                                                minnums: e.selectedTime.minnums,
                                                            },
                                                        }),
                                                            localStorage.setItem('orderQs', JSON.stringify(n)),
                                                            (e.loading = !0),
                                                            (t.next = 5),
                                                            e.$store.dispatch('logininfo').then(
                                                                (function () {
                                                                    var t = Object(r.a)(
                                                                        regeneratorRuntime.mark(function t(o) {
                                                                            return regeneratorRuntime.wrap(function (
                                                                                t
                                                                                ) {
                                                                                    for (;;)
                                                                                        switch ((t.prev = t.next)) {
                                                                                            case 0:
                                                                                                if (
                                                                                                    ((e.loading = !1),
                                                                                                    'A00004' !== o.code &&
                                                                                                    localStorage.removeItem(
                                                                                                        'orderQs'
                                                                                                    ),
                                                                                                    'A00006' !== o.code)
                                                                                                ) {
                                                                                                    t.next = 47
                                                                                                    break
                                                                                                }
                                                                                                if ('user' !== e.accountType) {
                                                                                                    t.next = 11
                                                                                                    break
                                                                                                }
                                                                                                if (8 !== e.userInfo.status) {
                                                                                                    t.next = 7
                                                                                                    break
                                                                                                }
                                                                                                return (
                                                                                                    e.$alert(
                                                                                                        e.$t('JNT.session.message8'),
                                                                                                        e.$t('JNT.session.alertTitle'),
                                                                                                        {
                                                                                                            confirmButtonText: e.$t(
                                                                                                                'JNT.session.confirmButtonText'
                                                                                                            ),
                                                                                                            callback: function (e) {},
                                                                                                        }
                                                                                                    ),
                                                                                                        t.abrupt('return')
                                                                                                )
                                                                                            case 7:
                                                                                                if (e.userInfo.idnum) {
                                                                                                    t.next = 11
                                                                                                    break
                                                                                                }
                                                                                                return (
                                                                                                    e.$message.warning(
                                                                                                        e.$t('JNT.session.message9')
                                                                                                    ),
                                                                                                        e.$router.push({
                                                                                                            path: '/account/profile',
                                                                                                            query: {
                                                                                                                orderQs: btoa(
                                                                                                                    JSON.stringify(n)
                                                                                                                ),
                                                                                                            },
                                                                                                        }),
                                                                                                        t.abrupt('return')
                                                                                                )
                                                                                            case 11:
                                                                                                if ('tg' !== e.accountType) {
                                                                                                    t.next = 30
                                                                                                    break
                                                                                                }
                                                                                                if (
                                                                                                    e.userInfo.taname &&
                                                                                                    'null' !== e.userInfo.taname
                                                                                                ) {
                                                                                                    t.next = 15
                                                                                                    break
                                                                                                }
                                                                                                return (
                                                                                                    e
                                                                                                        .$confirm(
                                                                                                            e.$t('JNT.session.message10'),
                                                                                                            e.$t(
                                                                                                                'JNT.session.alertTitle'
                                                                                                            ),
                                                                                                            {
                                                                                                                confirmButtonText: e.$t(
                                                                                                                    'JNT.session.confirmButtonText'
                                                                                                                ),
                                                                                                                cancelButtonText: e.$t(
                                                                                                                    'JNT.session.cancelButtonText'
                                                                                                                ),
                                                                                                                type: 'warning',
                                                                                                            }
                                                                                                        )
                                                                                                        .then(function () {
                                                                                                            e.$router.push(
                                                                                                                '/account/tourOperator'
                                                                                                            )
                                                                                                        })
                                                                                                        .catch(function () {}),
                                                                                                        t.abrupt('return')
                                                                                                )
                                                                                            case 15:
                                                                                                if (1 !== o.tourGuide.joinstatus) {
                                                                                                    t.next = 18
                                                                                                    break
                                                                                                }
                                                                                                return (
                                                                                                    e.$alert(
                                                                                                        e.$t('JNT.session.message11'),
                                                                                                        e.$t('JNT.session.alertTitle'),
                                                                                                        {
                                                                                                            confirmButtonText: e.$t(
                                                                                                                'JNT.session.confirmButtonText'
                                                                                                            ),
                                                                                                            callback: function (e) {},
                                                                                                        }
                                                                                                    ),
                                                                                                        t.abrupt('return')
                                                                                                )
                                                                                            case 18:
                                                                                                if (
                                                                                                    7 !== o.travelagents_status &&
                                                                                                    0 !== o.travelagents_status
                                                                                                ) {
                                                                                                    t.next = 21
                                                                                                    break
                                                                                                }
                                                                                                return (
                                                                                                    e.$alert(
                                                                                                        e.$t('JNT.session.message12'),
                                                                                                        e.$t('JNT.session.alertTitle'),
                                                                                                        {
                                                                                                            confirmButtonText: e.$t(
                                                                                                                'JNT.session.confirmButtonText'
                                                                                                            ),
                                                                                                            callback: function (e) {},
                                                                                                        }
                                                                                                    ),
                                                                                                        t.abrupt('return')
                                                                                                )
                                                                                            case 21:
                                                                                                if (8 !== o.travelagents_status) {
                                                                                                    t.next = 24
                                                                                                    break
                                                                                                }
                                                                                                return (
                                                                                                    e.$alert(
                                                                                                        e.$t('JNT.session.message13'),
                                                                                                        e.$t('JNT.session.alertTitle'),
                                                                                                        {
                                                                                                            confirmButtonText: e.$t(
                                                                                                                'JNT.session.confirmButtonText'
                                                                                                            ),
                                                                                                            callback: function (e) {},
                                                                                                        }
                                                                                                    ),
                                                                                                        t.abrupt('return')
                                                                                                )
                                                                                            case 24:
                                                                                                return (
                                                                                                    (t.next = 26),
                                                                                                        e.$axios.$post(
                                                                                                            '/ajax?ugi=bookingorder&action=checkTourguideConflict',
                                                                                                            {
                                                                                                                usertype: e.accountType,
                                                                                                                eventssessionid:
                                                                                                                e.selectedTime
                                                                                                                    .eventssessionid,
                                                                                                            }
                                                                                                        )
                                                                                                )
                                                                                            case 26:
                                                                                                if ('A00006' === t.sent.code) {
                                                                                                    t.next = 30
                                                                                                    break
                                                                                                }
                                                                                                return (
                                                                                                    e.$alert(
                                                                                                        e.$t('JNT.session.message14'),
                                                                                                        e.$t('JNT.session.alertTitle'),
                                                                                                        {
                                                                                                            confirmButtonText: e.$t(
                                                                                                                'JNT.session.confirmButtonText'
                                                                                                            ),
                                                                                                            callback: function (e) {},
                                                                                                        }
                                                                                                    ),
                                                                                                        t.abrupt('return')
                                                                                                )
                                                                                            case 30:
                                                                                                if ('ta' !== e.accountType) {
                                                                                                    t.next = 37
                                                                                                    break
                                                                                                }
                                                                                                if (
                                                                                                    7 !== e.userInfo.status &&
                                                                                                    0 !== e.userInfo.status
                                                                                                ) {
                                                                                                    t.next = 34
                                                                                                    break
                                                                                                }
                                                                                                return (
                                                                                                    e.$alert(
                                                                                                        e.$t('JNT.session.message15'),
                                                                                                        e.$t('JNT.session.alertTitle'),
                                                                                                        {
                                                                                                            confirmButtonText: e.$t(
                                                                                                                'JNT.session.confirmButtonText'
                                                                                                            ),
                                                                                                            callback: function (e) {},
                                                                                                        }
                                                                                                    ),
                                                                                                        t.abrupt('return')
                                                                                                )
                                                                                            case 34:
                                                                                                if (8 !== e.userInfo.status) {
                                                                                                    t.next = 37
                                                                                                    break
                                                                                                }
                                                                                                return (
                                                                                                    e.$alert(
                                                                                                        e.$t('JNT.session.message16'),
                                                                                                        e.$t('JNT.session.alertTitle'),
                                                                                                        {
                                                                                                            confirmButtonText: e.$t(
                                                                                                                'JNT.session.confirmButtonText'
                                                                                                            ),
                                                                                                            callback: function (e) {},
                                                                                                        }
                                                                                                    ),
                                                                                                        t.abrupt('return')
                                                                                                )
                                                                                            case 37:
                                                                                                if (!e.show_notice) {
                                                                                                    t.next = 41
                                                                                                    break
                                                                                                }
                                                                                                ;(e.show_notice = !1), (t.next = 46)
                                                                                                break
                                                                                            case 41:
                                                                                                return (
                                                                                                    (e.show_notice = !0),
                                                                                                        (e.agreeTime = 5),
                                                                                                    e.agreeTimer &&
                                                                                                    clearInterval(e.agreeTimer),
                                                                                                        (e.agreeTimer = setInterval(
                                                                                                            function () {
                                                                                                                e.agreeTime > 0
                                                                                                                    ? e.agreeTime--
                                                                                                                    : ((e.agreeTime = 0),
                                                                                                                        clearInterval(
                                                                                                                            e.agreeTimer
                                                                                                                        ))
                                                                                                            },
                                                                                                            1e3
                                                                                                        )),
                                                                                                        t.abrupt('return')
                                                                                                )
                                                                                            case 46:
                                                                                                e.$router.push(n)
                                                                                            case 47:
                                                                                            case 'end':
                                                                                                return t.stop()
                                                                                        }
                                                                                },
                                                                                t)
                                                                        })
                                                                    )
                                                                    return function (e) {
                                                                        return t.apply(this, arguments)
                                                                    }
                                                                })()
                                                            )
                                                    )
                                                case 5:
                                                case 'end':
                                                    return t.stop()
                                            }
                                    }, t)
                                })
                            )()
                        },
                    },
                    head: function () {
                        var e = this
                        return {
                            titleTemplate: function (t) {
                                return ''.concat(t, ' - ').concat(e.title)
                            },
                        }
                    },
                },
                T = _,
                j = n(43),
                component = Object(j.a)(
                    T,
                    function () {
                        var e = this,
                            t = e._self._c
                        return t(
                            'div',
                            [
                                t('div', { staticClass: 'bg' }, [
                                    e._m(0),
                                    e._v(' '),
                                    t('div', { staticClass: 'content-wrap' }, [
                                        t('div', { staticClass: 'content' }, [
                                            t(
                                                'div',
                                                { staticClass: 'selectdate-pagemain' },
                                                [
                                                    t('div', { staticClass: 'steps-new-container' }, [
                                                        t('ul', { staticClass: 'steps' }, [
                                                            t('li', { staticClass: 'active' }, [
                                                                t('span', [
                                                                    e._v('1-' + e._s(e.$t('JNT.session.tip1'))),
                                                                ]),
                                                                e._v(' '),
                                                                t('div', { staticClass: 'line' }),
                                                            ]),
                                                            e._v(' '),
                                                            t('li', [
                                                                t('span', [
                                                                    e._v('2-' + e._s(e.$t('JNT.session.tip2'))),
                                                                ]),
                                                                e._v(' '),
                                                                t('div', { staticClass: 'line' }),
                                                            ]),
                                                            e._v(' '),
                                                            t('li', [
                                                                t('span', [
                                                                    e._v('3-' + e._s(e.$t('JNT.session.tip3'))),
                                                                ]),
                                                                e._v(' '),
                                                                t('div', { staticClass: 'line' }),
                                                            ]),
                                                            e._v(' '),
                                                            t('li', [
                                                                t('span', [
                                                                    e._v('4-' + e._s(e.$t('JNT.session.tip4'))),
                                                                ]),
                                                            ]),
                                                        ]),
                                                    ]),
                                                    e._v(' '),
                                                    t('client-only', [
                                                        e.userInfo
                                                            ? t(
                                                            'div',
                                                            [
                                                                0 === e.userInfo.joinstatus
                                                                    ? t('el-alert', {
                                                                        staticStyle: { 'margin-top': '40px' },
                                                                        attrs: {
                                                                            title: e.$t('JNT.joinstatus[0]'),
                                                                            'show-icon': '',
                                                                            type: 'error',
                                                                        },
                                                                    })
                                                                    : e._e(),
                                                                e._v(' '),
                                                                1 === e.userInfo.joinstatus
                                                                    ? t('el-alert', {
                                                                        staticStyle: { 'margin-top': '40px' },
                                                                        attrs: {
                                                                            title: e.$t('JNT.joinstatus[1]'),
                                                                            'show-icon': '',
                                                                            type: 'warning',
                                                                        },
                                                                    })
                                                                    : e._e(),
                                                                e._v(' '),
                                                                2 === e.userInfo.joinstatus &&
                                                                1 !== e.userInfo.breakoffstatus
                                                                    ? t('el-alert', {
                                                                        staticStyle: { 'margin-top': '40px' },
                                                                        attrs: {
                                                                            title: e.$t('JNT.joinstatus[2]'),
                                                                            'show-icon': '',
                                                                            type: 'success',
                                                                        },
                                                                    })
                                                                    : e._e(),
                                                                e._v(' '),
                                                                2 === e.userInfo.joinstatus &&
                                                                1 === e.userInfo.breakoffstatus
                                                                    ? t('el-alert', {
                                                                        staticStyle: {
                                                                            'margin-top': '40px',
                                                                            cursor: 'pointer',
                                                                        },
                                                                        attrs: {
                                                                            title: e.$t('JNT.joinstatus[3]'),
                                                                            'show-icon': '',
                                                                            type: 'warning',
                                                                        },
                                                                    })
                                                                    : e._e(),
                                                                e._v(' '),
                                                                3 === e.userInfo.joinstatus
                                                                    ? t('el-alert', {
                                                                        staticStyle: { 'margin-top': '40px' },
                                                                        attrs: {
                                                                            title: e.$t('JNT.joinstatus[4]'),
                                                                            'show-icon': '',
                                                                            type: 'warning',
                                                                        },
                                                                    })
                                                                    : e._e(),
                                                            ],
                                                            1
                                                            )
                                                            : e._e(),
                                                    ]),
                                                    e._v(' '),
                                                    t('div', { staticClass: 'selectdate-new' }, [
                                                        t('section', { staticClass: 'selectdate-date' }, [
                                                            t('ul', { staticClass: 'selectdate-week' }, [
                                                                t('li', { staticClass: 'day' }, [
                                                                    e._v(e._s(e.$t('JNT.session.Mon'))),
                                                                ]),
                                                                e._v(' '),
                                                                t('li', { staticClass: 'day' }, [
                                                                    e._v(e._s(e.$t('JNT.session.Tues'))),
                                                                ]),
                                                                e._v(' '),
                                                                t('li', { staticClass: 'day' }, [
                                                                    e._v(e._s(e.$t('JNT.session.Wed'))),
                                                                ]),
                                                                e._v(' '),
                                                                t('li', { staticClass: 'day' }, [
                                                                    e._v(e._s(e.$t('JNT.session.Thur'))),
                                                                ]),
                                                                e._v(' '),
                                                                t('li', { staticClass: 'day' }, [
                                                                    e._v(e._s(e.$t('JNT.session.Fri'))),
                                                                ]),
                                                                e._v(' '),
                                                                t('li', { staticClass: 'day' }, [
                                                                    e._v(e._s(e.$t('JNT.session.Sat'))),
                                                                ]),
                                                                e._v(' '),
                                                                t('li', { staticClass: 'day' }, [
                                                                    e._v(e._s(e.$t('JNT.session.Sun'))),
                                                                ]),
                                                            ]),
                                                            e._v(' '),
                                                            t(
                                                                'ul',
                                                                { staticClass: 'selectdate-day' },
                                                                e._l(e.dateList, function (n, o) {
                                                                    return t(
                                                                        'li',
                                                                        {
                                                                            key: o,
                                                                            staticClass: 'day',
                                                                            class: {
                                                                                today: n.isToday,
                                                                                disable:
                                                                                    n.disabled || !n.hasValidSession,
                                                                                selected: e.dateSelected === n,
                                                                            },
                                                                            on: {
                                                                                click: function (t) {
                                                                                    return e.handleDateSelected(n)
                                                                                },
                                                                            },
                                                                        },
                                                                        [
                                                                            t('div', { staticClass: 'day-box' }, [
                                                                                t('p', [
                                                                                    t('span', { staticClass: 'yy' }, [
                                                                                        e._v(e._s(n.date.split('-')[1])),
                                                                                    ]),
                                                                                    e._v(' '),
                                                                                    t('span', { staticClass: 'num' }, [
                                                                                        e._v(e._s(n.date.split('-')[2])),
                                                                                    ]),
                                                                                ]),
                                                                                e._v(' '),
                                                                                t(
                                                                                    'span',
                                                                                    { staticClass: 'state' },
                                                                                    [
                                                                                        n.disabled || !n.hasValidSession
                                                                                            ? [
                                                                                                e._v(
                                                                                                    e._s(
                                                                                                        e.$t(
                                                                                                            'JNT.session.noAppointment'
                                                                                                        )
                                                                                                    )
                                                                                                ),
                                                                                            ]
                                                                                            : -1 === n.remaining_total &&
                                                                                            1 === n.remaining_check
                                                                                            ? [
                                                                                                e._v(
                                                                                                    e._s(
                                                                                                        e.$t(
                                                                                                            'JNT.session.canAppointment'
                                                                                                        )
                                                                                                    )
                                                                                                ),
                                                                                            ]
                                                                                            : n.remaining_total > 0
                                                                                                ? [
                                                                                                    e._v(
                                                                                                        '\n                        ' +
                                                                                                        e._s(
                                                                                                            e.$t(
                                                                                                                'JNT.session.surplus'
                                                                                                            )
                                                                                                        ) +
                                                                                                        e._s(n.remaining_total) +
                                                                                                        '\n                      '
                                                                                                    ),
                                                                                                ]
                                                                                                : [
                                                                                                    e._v(
                                                                                                        e._s(
                                                                                                            e.$t('JNT.session.full')
                                                                                                        )
                                                                                                    ),
                                                                                                ],
                                                                                    ],
                                                                                    2
                                                                                ),
                                                                            ]),
                                                                        ]
                                                                    )
                                                                }),
                                                                0
                                                            ),
                                                        ]),
                                                        e._v(' '),
                                                        e.dateSelected.sessions &&
                                                        e.dateSelected.sessions.length > 0
                                                            ? t(
                                                            'section',
                                                            { staticClass: 'selectdate-time' },
                                                            [
                                                                t(
                                                                    'ul',
                                                                    { staticClass: 'time-box' },
                                                                    e._l(
                                                                        e.dateSelected.sessions,
                                                                        function (n) {
                                                                            return t(
                                                                                'li',
                                                                                {
                                                                                    key: n.eventssessionid,
                                                                                    staticClass: 'times',
                                                                                    class: {
                                                                                        selected: e.selectedTime == n,
                                                                                        disabled:
                                                                                            !n.status ||
                                                                                            2 === n.status ||
                                                                                            0 === n.remaining ||
                                                                                            0 === n.remaining_check ||
                                                                                            n.isBefore ||
                                                                                            e.eventssessionid ===
                                                                                            n.eventssessionid,
                                                                                    },
                                                                                    on: {
                                                                                        click: function (t) {
                                                                                            return e.selectTime(n)
                                                                                        },
                                                                                    },
                                                                                },
                                                                                [
                                                                                    t('p', { staticClass: 'time' }, [
                                                                                        e._v(
                                                                                            e._s(n.begintime) +
                                                                                            '~' +
                                                                                            e._s(n.endtime)
                                                                                        ),
                                                                                    ]),
                                                                                    e._v(' '),
                                                                                    t(
                                                                                        'p',
                                                                                        { staticClass: 'state' },
                                                                                        [
                                                                                            e.eventssessionid ===
                                                                                            n.eventssessionid
                                                                                                ? [
                                                                                                    e._v(
                                                                                                        e._s(
                                                                                                            e.$t(
                                                                                                                'JNT.session.appointment'
                                                                                                            )
                                                                                                        )
                                                                                                    ),
                                                                                                ]
                                                                                                : n.isBefore
                                                                                                ? [
                                                                                                    e._v(
                                                                                                        e._s(
                                                                                                            e.$t(
                                                                                                                'JNT.session.noAppointment'
                                                                                                            )
                                                                                                        )
                                                                                                    ),
                                                                                                ]
                                                                                                : n.status
                                                                                                    ? 2 === n.status
                                                                                                        ? [
                                                                                                            e._v(
                                                                                                                e._s(
                                                                                                                    e.$t(
                                                                                                                        'JNT.session.full'
                                                                                                                    )
                                                                                                                )
                                                                                                            ),
                                                                                                        ]
                                                                                                        : -1 === n.remaining &&
                                                                                                        1 === n.remaining_check
                                                                                                            ? [
                                                                                                                e._v(
                                                                                                                    e._s(
                                                                                                                        e.$t(
                                                                                                                            'JNT.session.canAppointment'
                                                                                                                        )
                                                                                                                    )
                                                                                                                ),
                                                                                                            ]
                                                                                                            : n.remaining > 0
                                                                                                                ? [
                                                                                                                    e._v(
                                                                                                                        '\n                      ' +
                                                                                                                        e._s(
                                                                                                                            e.$t(
                                                                                                                                'JNT.session.surplus'
                                                                                                                            )
                                                                                                                        ) +
                                                                                                                        ' ' +
                                                                                                                        e._s(
                                                                                                                            n.remaining
                                                                                                                        ) +
                                                                                                                        '\n                    '
                                                                                                                    ),
                                                                                                                ]
                                                                                                                : [
                                                                                                                    e._v(
                                                                                                                        e._s(
                                                                                                                            e.$t(
                                                                                                                                'JNT.session.full'
                                                                                                                            )
                                                                                                                        )
                                                                                                                    ),
                                                                                                                ]
                                                                                                    : [
                                                                                                        e._v(
                                                                                                            e._s(
                                                                                                                e.$t(
                                                                                                                    'JNT.session.notReleased'
                                                                                                                )
                                                                                                            )
                                                                                                        ),
                                                                                                    ],
                                                                                        ],
                                                                                        2
                                                                                    ),
                                                                                ]
                                                                            )
                                                                        }
                                                                    ),
                                                                    0
                                                                ),
                                                                e._v(' '),
                                                                e.orderData
                                                                    ? t(
                                                                    'div',
                                                                    { staticClass: 'refund-list' },
                                                                    [
                                                                        t(
                                                                            'div',
                                                                            {
                                                                                staticClass:
                                                                                    'maintitle selectdate-h3',
                                                                                staticStyle: {
                                                                                    'margin-top': '30px',
                                                                                    'margin-bottom': '30px',
                                                                                },
                                                                            },
                                                                            [
                                                                                t('span', [
                                                                                    e._v(
                                                                                        e._s(
                                                                                            e.$t(
                                                                                                'JNT.session.orderChangeTitle'
                                                                                            )
                                                                                        )
                                                                                    ),
                                                                                ]),
                                                                            ]
                                                                        ),
                                                                        e._v(' '),
                                                                        t(
                                                                            'table',
                                                                            {
                                                                                staticClass:
                                                                                    'common-table record-table',
                                                                            },
                                                                            [
                                                                                t(
                                                                                    'thead',
                                                                                    {
                                                                                        staticClass:
                                                                                            'common-table-thead',
                                                                                    },
                                                                                    [
                                                                                        t('tr', [
                                                                                            t('th', [
                                                                                                t('span', [
                                                                                                    e._v(
                                                                                                        e._s(
                                                                                                            e.$t(
                                                                                                                'JNT.session.name'
                                                                                                            )
                                                                                                        )
                                                                                                    ),
                                                                                                ]),
                                                                                            ]),
                                                                                            e._v(' '),
                                                                                            t('th', [
                                                                                                e._v(
                                                                                                    e._s(
                                                                                                        e.$t(
                                                                                                            'JNT.session.IDNum'
                                                                                                        )
                                                                                                    )
                                                                                                ),
                                                                                            ]),
                                                                                            e._v(' '),
                                                                                            t('th', [
                                                                                                e._v(
                                                                                                    e._s(
                                                                                                        e.$t(
                                                                                                            'JNT.session.lookDate'
                                                                                                        )
                                                                                                    )
                                                                                                ),
                                                                                            ]),
                                                                                            e._v(' '),
                                                                                            t('th', [
                                                                                                e._v(
                                                                                                    e._s(
                                                                                                        e.$t(
                                                                                                            'JNT.session.session'
                                                                                                        )
                                                                                                    )
                                                                                                ),
                                                                                            ]),
                                                                                            e._v(' '),
                                                                                            t('th', [
                                                                                                e._v(
                                                                                                    e._s(
                                                                                                        e.$t(
                                                                                                            'JNT.session.queueArea'
                                                                                                        )
                                                                                                    )
                                                                                                ),
                                                                                            ]),
                                                                                            e._v(' '),
                                                                                            t('th', [
                                                                                                e._v(
                                                                                                    e._s(
                                                                                                        e.$t(
                                                                                                            'JNT.session.QRcode'
                                                                                                        )
                                                                                                    )
                                                                                                ),
                                                                                            ]),
                                                                                        ]),
                                                                                    ]
                                                                                ),
                                                                                e._v(' '),
                                                                                t(
                                                                                    'tbody',
                                                                                    {
                                                                                        staticClass:
                                                                                            'common-table-tbody',
                                                                                    },
                                                                                    e._l(
                                                                                        e.orderData.ticketcodes.filter(
                                                                                            function (e) {
                                                                                                return 3 !== e.status
                                                                                            }
                                                                                        ),
                                                                                        function (o) {
                                                                                            return t(
                                                                                                'tr',
                                                                                                { key: o.tcid },
                                                                                                [
                                                                                                    t('td', [
                                                                                                        e._v(
                                                                                                            '\n                        ' +
                                                                                                            e._s(
                                                                                                                e._f(
                                                                                                                    'hideName'
                                                                                                                )(o.realname)
                                                                                                            ) +
                                                                                                            '\n                      '
                                                                                                        ),
                                                                                                    ]),
                                                                                                    e._v(' '),
                                                                                                    t('td', [
                                                                                                        e._v(
                                                                                                            e._s(
                                                                                                                e._f('hideNum')(
                                                                                                                    o.idnum
                                                                                                                )
                                                                                                            )
                                                                                                        ),
                                                                                                    ]),
                                                                                                    e._v(' '),
                                                                                                    t('td', [
                                                                                                        e._v(
                                                                                                            e._s(o.eventsdate)
                                                                                                        ),
                                                                                                    ]),
                                                                                                    e._v(' '),
                                                                                                    t('td', [
                                                                                                        e._v(
                                                                                                            '\n                        ' +
                                                                                                            e._s(
                                                                                                                o.eventssession_begintime
                                                                                                            ) +
                                                                                                            '\n                        -\n                        ' +
                                                                                                            e._s(
                                                                                                                o.eventssession_endtime
                                                                                                            ) +
                                                                                                            '\n                      '
                                                                                                        ),
                                                                                                    ]),
                                                                                                    e._v(' '),
                                                                                                    t('td', [
                                                                                                        e._v(
                                                                                                            e._s(o.areaname)
                                                                                                        ),
                                                                                                    ]),
                                                                                                    e._v(' '),
                                                                                                    t('td', [
                                                                                                        t(
                                                                                                            'div',
                                                                                                            {
                                                                                                                staticClass:
                                                                                                                    'qrcode-box',
                                                                                                            },
                                                                                                            [
                                                                                                                t(
                                                                                                                    'el-popover',
                                                                                                                    {
                                                                                                                        attrs: {
                                                                                                                            placement:
                                                                                                                                'right',
                                                                                                                            width:
                                                                                                                                '160',
                                                                                                                            trigger:
                                                                                                                                'hover',
                                                                                                                        },
                                                                                                                    },
                                                                                                                    [
                                                                                                                        t('img', {
                                                                                                                            staticStyle:
                                                                                                                                {
                                                                                                                                    cursor:
                                                                                                                                        'pointer',
                                                                                                                                    width:
                                                                                                                                        '30px',
                                                                                                                                },
                                                                                                                            attrs: {
                                                                                                                                slot: 'reference',
                                                                                                                                src: n(
                                                                                                                                    675
                                                                                                                                ),
                                                                                                                            },
                                                                                                                            slot: 'reference',
                                                                                                                        }),
                                                                                                                        e._v(' '),
                                                                                                                        t(
                                                                                                                            'vue-qr',
                                                                                                                            {
                                                                                                                                staticClass:
                                                                                                                                    'qrcode',
                                                                                                                                attrs: {
                                                                                                                                    text: ''
                                                                                                                                        .concat(
                                                                                                                                            e.baseURL,
                                                                                                                                            '/g/'
                                                                                                                                        )
                                                                                                                                        .concat(
                                                                                                                                            o.ticketcode
                                                                                                                                        ),
                                                                                                                                    callback:
                                                                                                                                        function (
                                                                                                                                            e
                                                                                                                                        ) {
                                                                                                                                            o.ticketqrcode =
                                                                                                                                                e
                                                                                                                                        },
                                                                                                                                    size: 134,
                                                                                                                                    margin: 0,
                                                                                                                                },
                                                                                                                            }
                                                                                                                        ),
                                                                                                                        e._v(' '),
                                                                                                                        t(
                                                                                                                            'div',
                                                                                                                            {
                                                                                                                                staticStyle:
                                                                                                                                    {
                                                                                                                                        'text-align':
                                                                                                                                            'center',
                                                                                                                                    },
                                                                                                                            },
                                                                                                                            [
                                                                                                                                t(
                                                                                                                                    'el-link',
                                                                                                                                    {
                                                                                                                                        attrs:
                                                                                                                                            {
                                                                                                                                                type: 'primary',
                                                                                                                                                icon: 'el-icon-download',
                                                                                                                                                href: o.ticketqrcode,
                                                                                                                                                download:
                                                                                                                                                    'PERSONAL' ===
                                                                                                                                                    e
                                                                                                                                                        .orderData
                                                                                                                                                        .order
                                                                                                                                                        .fromtype
                                                                                                                                                        ? o.ticketcode
                                                                                                                                                        : e
                                                                                                                                                            .orderData
                                                                                                                                                            .order
                                                                                                                                                            .orderno,
                                                                                                                                            },
                                                                                                                                    },
                                                                                                                                    [
                                                                                                                                        e._v(
                                                                                                                                            e._s(
                                                                                                                                                e.$t(
                                                                                                                                                    'JNT.session.saveImg'
                                                                                                                                                )
                                                                                                                                            )
                                                                                                                                        ),
                                                                                                                                    ]
                                                                                                                                ),
                                                                                                                            ],
                                                                                                                            1
                                                                                                                        ),
                                                                                                                    ],
                                                                                                                    1
                                                                                                                ),
                                                                                                            ],
                                                                                                            1
                                                                                                        ),
                                                                                                    ]),
                                                                                                ]
                                                                                            )
                                                                                        }
                                                                                    ),
                                                                                    0
                                                                                ),
                                                                            ]
                                                                        ),
                                                                    ]
                                                                    )
                                                                    : e._e(),
                                                            ]
                                                            )
                                                            : e._e(),
                                                        e._v(' '),
                                                        t(
                                                            'div',
                                                            { staticClass: 'btn-group' },
                                                            [
                                                                e.orderid
                                                                    ? t(
                                                                    'el-button',
                                                                    {
                                                                        staticStyle: { 'min-width': '200px' },
                                                                        attrs: {
                                                                            type: 'primary',
                                                                            disabled:
                                                                                !e.dateSelected || !e.selectedTime,
                                                                        },
                                                                        on: { click: e.rebook },
                                                                    },
                                                                    [
                                                                        e._v(
                                                                            '\n                ' +
                                                                            e._s(
                                                                                e.$t('JNT.session.confirmChange')
                                                                            ) +
                                                                            '\n              '
                                                                        ),
                                                                    ]
                                                                    )
                                                                    : t(
                                                                    'el-button',
                                                                    {
                                                                        staticStyle: { 'min-width': '200px' },
                                                                        attrs: {
                                                                            type: 'primary',
                                                                            loading: e.loading,
                                                                            disabled:
                                                                                !e.dateSelected || !e.selectedTime,
                                                                        },
                                                                        on: { click: e.handleWriteOrder },
                                                                    },
                                                                    [
                                                                        'user' === e.type
                                                                            ? [
                                                                                e._v(
                                                                                    e._s(
                                                                                        e.$t(
                                                                                            'JNT.session.overbookingUser'
                                                                                        )
                                                                                    )
                                                                                ),
                                                                            ]
                                                                            : e._e(),
                                                                        e._v(' '),
                                                                        'tg' === e.type
                                                                            ? [
                                                                                e._v(
                                                                                    e._s(
                                                                                        e.$t(
                                                                                            'JNT.session.overbookingTg'
                                                                                        )
                                                                                    )
                                                                                ),
                                                                            ]
                                                                            : e._e(),
                                                                        e._v(' '),
                                                                        'ta' === e.type
                                                                            ? [
                                                                                e._v(
                                                                                    e._s(
                                                                                        e.$t(
                                                                                            'JNT.session.overbookingTa'
                                                                                        )
                                                                                    )
                                                                                ),
                                                                            ]
                                                                            : e._e(),
                                                                    ],
                                                                    2
                                                                    ),
                                                            ],
                                                            1
                                                        ),
                                                    ]),
                                                ],
                                                1
                                            ),
                                            e._v(' '),
                                            t('div', { staticClass: 'xz-wrap' }, [
                                                t('div', { staticClass: 'xz-head' }, [
                                                    t('span', [
                                                        e._v(e._s(e.$t('JNT.session.bookingInfo'))),
                                                    ]),
                                                ]),
                                                e._v(' '),
                                                t('div', {
                                                    staticClass: 'xz-content',
                                                    domProps: { innerHTML: e._s(e.$store.state.notice) },
                                                }),
                                            ]),
                                        ]),
                                    ]),
                                ]),
                                e._v(' '),
                                t(
                                    'client-only',
                                    [
                                        e.$store.state.notice
                                            ? t(
                                            'el-dialog',
                                            {
                                                attrs: {
                                                    'custom-class': 'dialog-modal',
                                                    visible: e.show_notice,
                                                },
                                                on: {
                                                    'update:visible': function (t) {
                                                        e.show_notice = t
                                                    },
                                                },
                                                scopedSlots: e._u(
                                                    [
                                                        {
                                                            key: 'footer',
                                                            fn: function () {
                                                                return [
                                                                    t(
                                                                        'div',
                                                                        {
                                                                            staticStyle: { 'text-align': 'left' },
                                                                        },
                                                                        [
                                                                            t(
                                                                                'el-checkbox',
                                                                                {
                                                                                    model: {
                                                                                        value: e.agree,
                                                                                        callback: function (t) {
                                                                                            e.agree = t
                                                                                        },
                                                                                        expression: 'agree',
                                                                                    },
                                                                                },
                                                                                [
                                                                                    e._v(
                                                                                        e._s(
                                                                                            e.$t(
                                                                                                'JNT.session.hasReadAndAgreed'
                                                                                            )
                                                                                        )
                                                                                    ),
                                                                                ]
                                                                            ),
                                                                        ],
                                                                        1
                                                                    ),
                                                                    e._v(' '),
                                                                    t(
                                                                        'div',
                                                                        { staticClass: 'btn-group' },
                                                                        [
                                                                            t(
                                                                                'el-button',
                                                                                {
                                                                                    staticStyle: {
                                                                                        'min-width': '200px',
                                                                                    },
                                                                                    attrs: {
                                                                                        disabled:
                                                                                            !e.agree || e.agreeTime > 0,
                                                                                        type: 'primary',
                                                                                    },
                                                                                    on: { click: e.handleWriteOrder },
                                                                                },
                                                                                [
                                                                                    e._v(
                                                                                        e._s(
                                                                                            e.agree && 0 === e.agreeTime
                                                                                                ? e.$t(
                                                                                                'JNT.session.confirmButtonText'
                                                                                                )
                                                                                                : e.$t(
                                                                                                'JNT.session.readAndAgreed'
                                                                                                )
                                                                                        ) + ' '
                                                                                    ),
                                                                                    e.agreeTime > 0
                                                                                        ? t('span', [
                                                                                            e._v(e._s(e.agreeTime)),
                                                                                        ])
                                                                                        : e._e(),
                                                                                ]
                                                                            ),
                                                                        ],
                                                                        1
                                                                    ),
                                                                ]
                                                            },
                                                            proxy: !0,
                                                        },
                                                    ],
                                                    null,
                                                    !1,
                                                    584241317
                                                ),
                                            },
                                            [
                                                t(
                                                    'div',
                                                    { attrs: { slot: 'title' }, slot: 'title' },
                                                    [
                                                        t('h5', { staticClass: 'header' }, [
                                                            e._v(e._s(e.$t('JNT.session.bookingInfo'))),
                                                        ]),
                                                    ]
                                                ),
                                                e._v(' '),
                                                t('div', {
                                                    staticStyle: {
                                                        height: '400px',
                                                        overflow: 'auto',
                                                        'white-space': 'pre-wrap',
                                                        'line-height': '2',
                                                    },
                                                    domProps: {
                                                        innerHTML: e._s(e.$store.state.notice),
                                                    },
                                                }),
                                            ]
                                            )
                                            : e._e(),
                                    ],
                                    1
                                ),
                                e._v(' '),
                                t('Verify', {
                                    ref: 'verify',
                                    attrs: {
                                        mode: 'pop',
                                        captchaType: e.captchaType,
                                        'img-size': { width: '330px', height: '155px' },
                                    },
                                    on: { success: e.success },
                                }),
                                e._v(' '),
                                t('Bulletin', { attrs: { show: '' } }),
                            ],
                            1
                        )
                    },
                    [
                        function () {
                            var e = this._self._c
                            return e('div', { staticClass: 'banner inner' }, [
                                e('div', { staticClass: 'banner-img' }),
                            ])
                        },
                    ],
                    !1,
                    null,
                    null,
                    null
                )
            t.default = component.exports
            installComponents(component, { Bulletin: n(657).default })
        },
    },
])
